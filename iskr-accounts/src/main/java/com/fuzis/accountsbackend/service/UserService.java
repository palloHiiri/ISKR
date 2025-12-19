package com.fuzis.accountsbackend.service;

import com.fuzis.accountsbackend.entity.User;
import com.fuzis.accountsbackend.entity.UserProfile;
import com.fuzis.accountsbackend.entity.enumerate.UserStatus;
import com.fuzis.accountsbackend.repository.UserProfileRepository;
import com.fuzis.accountsbackend.repository.UserRepository;
import com.fuzis.accountsbackend.transfer.ChangeDTO;
import com.fuzis.accountsbackend.transfer.SelectDTO;
import com.fuzis.accountsbackend.transfer.state.State;
import com.fuzis.accountsbackend.util.IntegrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class UserService
{
    private final UserRepository userRepository;

    private final UserProfileRepository userProfileRepository;

    private final IntegrationRequest  integrationRequest;

    public UserService(@Autowired UserRepository userRepository,
                       @Autowired UserProfileRepository userProfileRepository,
                       @Autowired IntegrationRequest integrationRequest){
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.integrationRequest = integrationRequest;
    }

    public SelectDTO<User> getUserDataByLogin(String login){
        try {
            Optional<UserProfile> user = userProfileRepository.findByUsernameOrEmail(login);
            return user
                    .map(value -> new SelectDTO<>(State.OK, value.getUser(), ""))
                    .orElseGet(() -> new SelectDTO<>(State.Fail_NotFound, null, "User not found"));
        }
        catch (Exception e){
            return new SelectDTO<>(State.Fail, null, "Unexpected error: " + e.getMessage());
        }
    }

    public ChangeDTO<User> getUserData(Integer userId){
        try {
            Optional<User> user = userRepository.findById(userId);
            return user
                    .map(value -> new ChangeDTO<>(State.OK, "User found", value ))
                    .orElseGet(() -> new ChangeDTO<>(State.Fail_NotFound, "User not found",null));
        }
        catch (Exception e){
            return new ChangeDTO<>(State.Fail,  "Unexpected error: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Integer> deleteUser(Integer userId){
        try {
            Optional<User> user = userRepository.findById(userId);
            if(user.isEmpty())return new ChangeDTO<>(State.Fail_NotFound, "No user found", null);
            userRepository.deleteById(userId);
            return new ChangeDTO<>(State.OK, "User deleted successfully", null);
        }
        catch (Exception e){
            return new ChangeDTO<>(State.Fail, "Unexpected error: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> changeUsername(Integer userId, String new_username){
        Optional<User> other_user = userRepository.findUserByUsername(new_username);
        if (other_user.isPresent() && other_user.get().getUser_id().equals(userId)) {
            return new ChangeDTO<>(State.Fail_Conflict, "Same username", null);
        }
        if (other_user.isPresent()) return new ChangeDTO<>(State.Fail_Conflict, "Username already taken", null);
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            return new ChangeDTO<>(State.Fail, "No user found", null);
        }
        user.get().setUsername(new_username);
        userRepository.save(user.get());
        return updateUserSSO(userId, "New-Username", new_username, "update-user-sso");
    }

    public ChangeDTO<Object> changeNickname(Integer userId, String new_nickname){
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            return new ChangeDTO<>(State.Fail, "No user found", null);
        }
        var profile = user.get().getProfile();
        profile.setNickname(new_nickname);
        userProfileRepository.save(profile);
        return updateUserSSO(userId, "New-Nickname", new_nickname, "update-user-sso");
    }

    public ChangeDTO<Object> changePassword(Integer userId, String new_password){
        return updateUserSSO(userId, "New-Password", new_password, "change-password-sso");
    }

    public ChangeDTO<Object> changeEmail(Integer userId, String new_email){
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            return new ChangeDTO<>(State.Fail, "No user found", null);
        }
        var profile = user.get().getProfile();
        profile.setEmail(new_email);
        profile.setEmail_verified(false);
        userProfileRepository.save(profile);
        var sso_res = updateUserSSO(userId, "New-Email", new_email, "update-user-sso");
        if(sso_res.getState() != State.OK){
            return sso_res;
        }
        MultiValueMap<String, String> email_verify_body = new LinkedMultiValueMap<>();
        email_verify_body.add("X-User-Id", userId.toString());
        var response_sso = integrationRequest.sendPostRequestIntegration("v1/accounts/verify-email", email_verify_body);
        if (response_sso.getStatusCode() != HttpStatus.OK) {
            return new ChangeDTO<>(State.Fail, "Unable to send email verification", response_sso.getBody());
        }
        return new ChangeDTO<>(State.OK, "Email changed, verification code sent", null);
    }

    public ChangeDTO<Object> updateUserSSO(Integer userId, String key, String value, String endpoint){
        try {
            MultiValueMap<String, String> sso_change_body = new LinkedMultiValueMap<>();
            sso_change_body.add("X-User-Id", userId.toString());
            if(key != null) sso_change_body.add(key, value);
            var response_sso = integrationRequest.sendPostRequestIntegration("v1/accounts/"+endpoint, sso_change_body);
            if (response_sso.getStatusCode() != HttpStatus.NO_CONTENT &&  response_sso.getStatusCode() != HttpStatus.OK) {
                return new ChangeDTO<>(State.Fail, "Unable to update field", response_sso.getBody());
            }
            return new ChangeDTO<>(State.OK, "Field changed successfully", null);
        }
        catch (RestClientException e) {
            return new ChangeDTO<>(State.Fail, "Unable to connect to server, error: " + e.getMessage(), null);
        }
        catch (Exception e){
            return new ChangeDTO<>(State.Fail, "Unexpected error: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> banUser(Integer userId){
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            return new ChangeDTO<>(State.Fail, "No user found", null);
        }
        var profile = user.get().getProfile();
        profile.setStatus(UserStatus.banned);
        userProfileRepository.save(profile);
        return updateUserSSO(userId, "X-Account-Banned", "true", "update-account-state-sso");
    }

    public ChangeDTO<Object> unBanUser(Integer userId){
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            return new ChangeDTO<>(State.Fail, "No user found", null);
        }
        var profile = user.get().getProfile();
        profile.setStatus(UserStatus.notBanned);
        userProfileRepository.save(profile);
        return updateUserSSO(userId, "X-Account-Banned", "false", "update-account-state-sso");
    }

    public ChangeDTO<Object> changeDescription(Integer userId, String new_description){
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            return new ChangeDTO<>(State.Fail, "No user found", null);
        }
        var profile = user.get().getProfile();
        profile.setProfile_description(new_description);
        userProfileRepository.save(profile);
        return new  ChangeDTO<>(State.OK, "Field changed successfully", null);
    }

    public ChangeDTO<Object> changeBirthDate(Integer userId, String new_birth_date){
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            return new ChangeDTO<>(State.Fail, "No user found", null);
        }
        var profile = user.get().getProfile();
        profile.setBirth_date(ZonedDateTime.parse(new_birth_date));
        userProfileRepository.save(profile);
        return new  ChangeDTO<>(State.OK, "Field changed successfully", null);
    }

    public ChangeDTO<Object> createUserDB(String username, String nickname, String email){
        Optional<User> other_user = userRepository.findUserByUsername(username);
        if (other_user.isPresent()) return new ChangeDTO<>(State.Fail_Conflict, "Username already taken", null);
        User new_user = new User(username);
        userRepository.save(new_user);
        UserProfile new_user_profile = new UserProfile(new_user, nickname, email);
        userProfileRepository.save(new_user_profile);
        return new ChangeDTO<>(State.OK, "", new_user.getUser_id());
    }

    public ChangeDTO<Object> createUser(String username, String nickname, String email, String password){
        ChangeDTO<Object> res = createUserDB(username, nickname, email);
        if(res.getState() != State.OK) return res;
        Integer user_id = (Integer)res.getKey();
        try {
            MultiValueMap<String, String> sso_create_body = new LinkedMultiValueMap<>();
            sso_create_body.add("X-User-Id", user_id.toString());
            sso_create_body.add("Email", email);
            sso_create_body.add("Nickname", nickname);
            sso_create_body.add("Username", username);
            var response_sso = integrationRequest.sendPostRequestIntegration("v1/accounts/user/create-sso-account", sso_create_body);
            if (response_sso.getStatusCode() != HttpStatus.NO_CONTENT) {
                return new ChangeDTO<>(State.Fail, "Unable to create user sso", response_sso.getBody());
            }
            var update_password_res = updateUserSSO(user_id, "New-Password", password, "change-password-sso");
            if(update_password_res.getState() != State.OK){
                return new ChangeDTO<>(State.Fail, "Unable to set user password", update_password_res);
            }
            var verify_email_sent_res = updateUserSSO(user_id, null, null, "verify-email");
            if(verify_email_sent_res.getState() != State.OK){
                return new ChangeDTO<>(State.Fail, "Unable to sent email verification token", verify_email_sent_res);
            }
            return new ChangeDTO<>(State.OK, "User created. Verification mail sent.", user_id);
        }
        catch (RestClientException e) {
            return new ChangeDTO<>(State.Fail, "Unable to connect to server, error: " + e.getMessage(), null);
        }
        catch (Exception e){
            return new ChangeDTO<>(State.Fail, "Unexpected error: " + e.getMessage(), null);
        }
    }
}
