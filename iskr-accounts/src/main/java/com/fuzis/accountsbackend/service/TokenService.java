package com.fuzis.accountsbackend.service;

import com.fuzis.accountsbackend.entity.Token;
import com.fuzis.accountsbackend.entity.User;
import com.fuzis.accountsbackend.entity.UserProfile;
import com.fuzis.accountsbackend.messaging.RabbitSendService;
import com.fuzis.accountsbackend.repository.TokenRepository;
import com.fuzis.accountsbackend.repository.TokenTypeRepository;
import com.fuzis.accountsbackend.repository.UserProfileRepository;
import com.fuzis.accountsbackend.repository.UserRepository;
import com.fuzis.accountsbackend.transfer.ChangeDTO;
import com.fuzis.accountsbackend.transfer.SelectDTO;
import com.fuzis.accountsbackend.transfer.messaging.EmailDTO;
import com.fuzis.accountsbackend.transfer.messaging.EmailType;
import com.fuzis.accountsbackend.transfer.state.State;
import com.fuzis.accountsbackend.util.IntegrationRequest;
import com.fuzis.accountsbackend.util.TokenGenerator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Log4j2
public class TokenService {
    private final TokenRepository tokenRepository;
    private final RabbitSendService  rabbitSendService;
    private final UserRepository userRepository;
    private final TokenGenerator tokenGenerator;
    private final TokenTypeRepository tokenTypeRepository;
    private final IntegrationRequest integrationRequest;
    private final UserProfileRepository userProfileRepository;

    @Value("${token.base_expire}")
    private Integer token_base_expire;

    @Autowired
    public TokenService(TokenRepository tokenRepository,
                        RabbitSendService rabbitSendService,
                        UserRepository userRepository,
                        TokenGenerator tokenGenerator,
                        TokenTypeRepository tokenTypeRepository,
                        IntegrationRequest integrationRequest,
                        UserProfileRepository  userProfileRepository) {
        this.tokenRepository = tokenRepository;
        this.rabbitSendService = rabbitSendService;
        this.userRepository = userRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenTypeRepository = tokenTypeRepository;
        this.integrationRequest =  integrationRequest;
        this.userProfileRepository = userProfileRepository;
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
                    if(response.getStatusCode() != HttpStatus.NO_CONTENT){
                        return new ChangeDTO<>(State.Fail, "Unable to set email verification on sso", response.getBody());
                    }
                    user.get().getProfile().setEmail_verified(true);
                    userRepository.save(user.get());
                    return new ChangeDTO<>(State.OK, "Email successfully verified", null);
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
            Optional<User> user = userRepository.findById(Integer.parseInt(token.getTokenBody()));
            if(user.isPresent()) {
                return new ChangeDTO<>(State.OK, "Token granted", user);
            }
            else{
                return new ChangeDTO<>(State.Fail_BadData, "Invalid Token, User Not Found", null);
            }
        }
        else return new ChangeDTO<>(State.Fail_Not_Implemented, "Unknown token to redeem", null);
    }

    public ChangeDTO<Token> createToken(Integer userId, String type) {
        log.warn("createToken request get: " + userId);
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
        }
        return createTokenInner(user.get(), type);
    }

    public ChangeDTO<Token> createTokenInner(User user, String type) {
        try {
            if(this.tokenTypeRepository.getTokenTypeByttName(type) == null){
                return new ChangeDTO<>(State.Fail_Not_Implemented, "Unknown token type to create", null);
            }
            Token token = tokenRepository.save(new Token(tokenGenerator.getTokenKey(), ZonedDateTime.now().plusSeconds(token_base_expire),
                    this.tokenTypeRepository.getTokenTypeByttName(type), user.getUser_id().toString()));
            EmailType emailType = EmailType.getByTokenType(type);
            rabbitSendService.send_email(new EmailDTO<>(user.getProfile().getEmail(),
                    emailType,
                    token.getTokenKey()));
            return new ChangeDTO<>(State.OK, "Token sent", token);
        }
        catch (Exception e) {
            return new ChangeDTO<>(State.Fail, "Unknown error ("+e+") " + e.getMessage(), null);
        }
    }

    public ChangeDTO createResetToken(String login) {
        Optional<UserProfile> user = userProfileRepository.findByUsernameOrEmail(login);
        if(user.isEmpty()) {
            return new ChangeDTO<>(State.OK, "Token sent if user is present", null);
        }
        var ret_val = createTokenInner(user.get().getUser(), "reset_password_token");
        if(ret_val.getState() == State.OK)return new ChangeDTO<>(State.OK, "Token sent if user is present", null);
        return new ChangeDTO<>(State.Fail, "Unknown error", null);
    }
    public ChangeDTO redeemResetPasswordToken(String token, String password) {
        try {
            MultiValueMap<String, String> redeem_token_body = new LinkedMultiValueMap<>();
            redeem_token_body.add("Token", token);
            var response = integrationRequest.sendPostRequestIntegration("v1/accounts/redeem-token", redeem_token_body);
            if (response.getStatusCode() != HttpStatus.OK) {
                if(response.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return new ChangeDTO<>(State.Fail_NotFound, "Unknown token", response.getBody());
                } else if (response.getStatusCode() == HttpStatus.GONE) {
                    return new ChangeDTO<>(State.Fail_Expired, "Token expired", response.getBody());
                }
                return new ChangeDTO<>(State.Fail, "Unable to send redeem request", response.getBody());
            }
            if(response.getBody() == null || response.getBody().get("key") == null)
                return new ChangeDTO<>(State.Fail, "Unable to parse redeem token response", response.getBody());
            Integer res = (Integer)(((Map)response.getBody().get("key")).get("user_id"));
            MultiValueMap<String, String> sso_reset_password_body = new LinkedMultiValueMap<>();
            sso_reset_password_body.add("X-User-Id", res.toString());
            sso_reset_password_body.add("New-Password", password);
            var response_sso = integrationRequest.sendPostRequestIntegration("v1/accounts/change-password-sso", sso_reset_password_body);
            if (response_sso.getStatusCode() != HttpStatus.NO_CONTENT) {
                return new ChangeDTO<>(State.Fail, "Unable to update password", response_sso.getBody());
            }
            return new ChangeDTO<>(State.OK, "Password successfully updated", response_sso.getBody());
        }
        catch (RestClientException e) {
            return new ChangeDTO<>(State.Fail, "Unable to connect to server, error: " + e.getMessage(), null);
        }
        catch (Exception e) {
            return new ChangeDTO<>(State.Fail, "Service error: " + e.getMessage(), null);
        }
    }
}
