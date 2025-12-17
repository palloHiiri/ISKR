package com.fuzis.accountsbackend.controller;

import com.fuzis.accountsbackend.entity.User;
import com.fuzis.accountsbackend.service.UserService;
import com.fuzis.accountsbackend.transfer.ChangeDTO;
import com.fuzis.accountsbackend.transfer.IStateDTO;
import com.fuzis.accountsbackend.transfer.SelectDTO;
import com.fuzis.accountsbackend.util.HttpUtil;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/v1/accounts/user")
public class UserController {

    private final UserService userService;

    private final HttpUtil httpUtil;

    @Autowired
    public UserController( UserService userService, HttpUtil httpUtil) {
        this.userService =  userService;
        this.httpUtil = httpUtil;
    }

    @GetMapping("/{id}")
    public ResponseEntity<SelectDTO<User>> getUserData(@PathVariable @Min(0) Integer id) {
        return httpUtil.handleServiceResponse(userService.getUserData(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ChangeDTO<Integer>> deleteUser(@PathVariable @Min(0) Integer id){
        return httpUtil.handleServiceResponse(userService.deleteUser(id));
    }

    @PostMapping
    public ResponseEntity<ChangeDTO<Object>> createUser(@RequestParam @NotBlank String username,
                                                         @RequestParam @NotBlank String nickname,
                                                         @RequestParam @NotBlank String password,
                                                         @RequestParam @NotBlank String email){
        return httpUtil.handleServiceResponse(userService.createUser(username, nickname, email, password));
    }

    @PutMapping("/{id}/username")
    public ResponseEntity<ChangeDTO<Object>> changeUsername(@PathVariable @Min(0) Integer id, @RequestParam @NotBlank String username){
        return httpUtil.handleServiceResponse(userService.changeUsername(id, username));
    }

    @PutMapping("/{id}/nickname")
    public ResponseEntity<ChangeDTO<Object>> changeNickname(@PathVariable @Min(0) Integer id, @RequestParam @NotBlank String nickname){
        return httpUtil.handleServiceResponse(userService.changeNickname(id, nickname));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ChangeDTO<Object>> changePassword(@PathVariable @Min(0) Integer id, @RequestParam @NotBlank String password){
        return httpUtil.handleServiceResponse(userService.changePassword(id, password));
    }

    @PutMapping("/{id}/email")
    public ResponseEntity<ChangeDTO<Object>> changeEmail(@PathVariable @Min(0) Integer id, @RequestParam @NotBlank String email){
        return httpUtil.handleServiceResponse(userService.changeEmail(id, email));
    }

    @PutMapping("/{id}/description")
    public ResponseEntity<ChangeDTO<Object>> changeDescription(@PathVariable @Min(0) Integer id, @RequestParam @NotBlank String description){
        return httpUtil.handleServiceResponse(userService.changeDescription(id, description));
    }

    @PutMapping("/{id}/birth-date")
    public ResponseEntity<ChangeDTO<Object>> changeBirthDate(@PathVariable @Min(0) Integer id, @RequestParam @NotBlank String birth_date){
        return httpUtil.handleServiceResponse(userService.changeBirthDate(id, birth_date));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<ChangeDTO<Object>> banUser(@PathVariable @Min(0) Integer id){
        return httpUtil.handleServiceResponse(userService.banUser(id));
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<ChangeDTO<Object>> unBanUser(@PathVariable @Min(0) Integer id){
        return httpUtil.handleServiceResponse(userService.unBanUser(id));
    }
}
