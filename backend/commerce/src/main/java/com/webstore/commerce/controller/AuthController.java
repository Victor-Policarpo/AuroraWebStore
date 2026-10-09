package com.webstore.commerce.controller;

import com.webstore.commerce.dto.request.CreateUserRequest;
import com.webstore.commerce.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final LoginService loginService;

    @PostMapping("/register")
    public ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserRequest dto){
        loginService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
