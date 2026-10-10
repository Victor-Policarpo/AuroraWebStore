package com.webstore.commerce.controller;

import com.webstore.commerce.dto.request.CreateUserRequest;
import com.webstore.commerce.dto.request.LoginRequest;
import com.webstore.commerce.dto.response.LoginResponse;
import com.webstore.commerce.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final LoginService loginService;

    @PostMapping("/register")
    public ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserRequest dto){
        loginService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest dto){
        return ResponseEntity.status(HttpStatus.OK).body(loginService.login(dto));
    }

}
