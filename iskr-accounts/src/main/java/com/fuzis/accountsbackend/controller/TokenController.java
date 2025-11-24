package com.fuzis.accountsbackend.controller;

import com.fuzis.accountsbackend.entity.Token;
import com.fuzis.accountsbackend.service.TokenService;
import com.fuzis.accountsbackend.transfer.ChangeDTO;
import com.fuzis.accountsbackend.util.HttpUtil;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts/token")
public class TokenController {
    private final TokenService tokenService;

    private final HttpUtil httpUtil;

    @Autowired
    public TokenController(TokenService tokenService, HttpUtil httpUtil){
        this.tokenService = tokenService;
        this.httpUtil = httpUtil;
    }

    @PostMapping("/redeem")
    public ResponseEntity<ChangeDTO<Token>> redeemToken(@RequestParam @NotBlank String token) {
        return httpUtil.handleServiceResponse(tokenService.redeemToken(token));
    }

    @PostMapping
    public ResponseEntity<ChangeDTO<Token>> createToken(@RequestParam Integer userId, @RequestParam @NotBlank String type) {
        return httpUtil.handleServiceResponse(tokenService.createToken(userId, type));
    }
}
