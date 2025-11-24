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
import com.fuzis.accountsbackend.util.TokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class TokenService {
    private final TokenRepository tokenRepository;
    private final RabbitSendService  rabbitSendService;
    private final UserRepository userRepository;
    private final TokenGenerator tokenGenerator;
    private final UserProfileRepository userProfileRepository;
    private final TokenTypeRepository tokenTypeRepository;

    @Value("${token.base_expire}")
    private Integer token_base_expire;

    @Autowired
    public TokenService(TokenRepository tokenRepository,
                        RabbitSendService rabbitSendService,
                        UserRepository userRepository,
                        TokenGenerator tokenGenerator,
                        UserProfileRepository userProfileRepository,
                        TokenTypeRepository tokenTypeRepository) {
        this.tokenRepository = tokenRepository;
        this.rabbitSendService = rabbitSendService;
        this.userRepository = userRepository;
        this.tokenGenerator = tokenGenerator;
        this.userProfileRepository = userProfileRepository;
        this.tokenTypeRepository = tokenTypeRepository;
    }

    public ChangeDTO<Token> redeemToken(String token_key) {
        Token token = tokenRepository.findByTokenKey(token_key);
        if (token == null) {
            return new ChangeDTO<>(State.Fail_NotFound, "Not able to find the token to redeem", null);
        }
        if(Objects.equals(token.getTokenType().getTtName(), "verify_email_token")){

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
