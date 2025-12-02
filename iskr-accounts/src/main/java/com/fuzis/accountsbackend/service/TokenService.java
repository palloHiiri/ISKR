package com.fuzis.accountsbackend.service;

import com.fuzis.accountsbackend.entity.Token;
import com.fuzis.accountsbackend.entity.User;
import com.fuzis.accountsbackend.messaging.RabbitSendService;
import com.fuzis.accountsbackend.repository.TokenRepository;
import com.fuzis.accountsbackend.repository.TokenTypeRepository;
import com.fuzis.accountsbackend.repository.UserProfileRepository;
import com.fuzis.accountsbackend.repository.UserRepository;
import com.fuzis.accountsbackend.transfer.ChangeDTO;
import com.fuzis.accountsbackend.transfer.messaging.EmailDTO;
import com.fuzis.accountsbackend.transfer.messaging.EmailType;
import com.fuzis.accountsbackend.transfer.state.State;
import com.fuzis.accountsbackend.util.IntegrationRequest;
import com.fuzis.accountsbackend.util.TokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class TokenService {
    private final TokenRepository tokenRepository;
    private final RabbitSendService  rabbitSendService;
    private final UserRepository userRepository;
    private final TokenGenerator tokenGenerator;
    private final TokenTypeRepository tokenTypeRepository;
    private final IntegrationRequest integrationRequest;

    @Value("${token.base_expire}")
    private Integer token_base_expire;

    @Autowired
    public TokenService(TokenRepository tokenRepository,
                        RabbitSendService rabbitSendService,
                        UserRepository userRepository,
                        TokenGenerator tokenGenerator,
                        TokenTypeRepository tokenTypeRepository,
                        IntegrationRequest integrationRequest) {
        this.tokenRepository = tokenRepository;
        this.rabbitSendService = rabbitSendService;
        this.userRepository = userRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenTypeRepository = tokenTypeRepository;
        this.integrationRequest =  integrationRequest;
    }

    public ChangeDTO<Object> redeemToken(String token_key) {
        Token token = tokenRepository.findByTokenKey(token_key);
        if (token == null) {
            return new ChangeDTO<>(State.Fail_NotFound, "Not able to find the token to redeem", null);
        }
        if(ZonedDateTime.now().isAfter(token.getTill_date())){
            return new ChangeDTO<>(State.Fail_Expired, "Token is expired", null);
        }
        if(Objects.equals(token.getTokenType().getTtName(), "verify_email_token")){
            try {
                Optional<User> user = userRepository.findById(Integer.parseInt(token.getTokenBody()));
                if(user.isPresent()) {
                    MultiValueMap<String, String> sso_request_body = new LinkedMultiValueMap<>();
                    sso_request_body.add("X-User-Id", user.get().getUser_id().toString());
                    sso_request_body.add("Email-Verified", "true");
                    var response = integrationRequest.sendPostRequestIntegration("v1/accounts/verify-email-sso", sso_request_body);
                    if(response.getStatusCode() != HttpStatus.OK){
                        return new ChangeDTO<>(State.Fail, "Unable to set email verification on sso", response.getBody());
                    }
                    user.get().getProfile().setEmail_verified(true);
                }
                else{
                    return new ChangeDTO<>(State.Fail_BadData, "Invalid Token, User Not Found", null);
                }
            }
            catch (RestClientException e) {
                return new ChangeDTO<>(State.Fail_BadData, "Unable to connect to server, error: " + e.getMessage(), null);
            }
            catch (Exception e) {
                return new ChangeDTO<>(State.Fail_BadData, "Invalid Token", null);
            }
        }
        if(Objects.equals(token.getTokenType().getTtName(), "reset_password_token")){
            return new ChangeDTO<>(State.Fail_NotFound, "Unable to redeem password change token directly, use another endpoint", null);
        }
        return new ChangeDTO<>(State.Fail_Not_Implemented, "Unknown token to redeem", null);
    }

    public ChangeDTO<Token> createToken(Integer userId, String type) {
        try {
            Optional<User> user = userRepository.findById(userId);
            if(user.isEmpty()) {
                throw new RuntimeException("User not found");
            }
            if(this.tokenTypeRepository.getTokenTypeByttName(type) == null){
                return new ChangeDTO<>(State.Fail_Not_Implemented, "Unknown token type to create", null);
            }
            Token token = tokenRepository.save(new Token(tokenGenerator.getTokenKey(), ZonedDateTime.now().plusSeconds(token_base_expire),
                    this.tokenTypeRepository.getTokenTypeByttName(type), userId.toString()));
            EmailType emailType = EmailType.getByTokenType(type);
            rabbitSendService.send_email(new EmailDTO<>(user.get().getProfile().getEmail(),
                    emailType,
                    token.getTokenKey()));
            return new ChangeDTO<>(State.OK, "Token sent", token);
        }
        catch (Exception e) {
            return new ChangeDTO<>(State.Fail, "Unknown error ("+e+") " + e.getMessage(), null);
        }

    }
}
