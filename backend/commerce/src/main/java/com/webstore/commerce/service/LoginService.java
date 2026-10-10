package com.webstore.commerce.service;

import com.webstore.commerce.dto.request.CreateUserRequest;
import com.webstore.commerce.dto.request.LoginRequest;
import com.webstore.commerce.dto.response.LoginResponse;
import com.webstore.commerce.entity.Role;
import com.webstore.commerce.entity.User;
import com.webstore.commerce.exception.errors.BadCredentialsException;
import com.webstore.commerce.mapper.UserMapper;
import com.webstore.commerce.repository.UserRepository;
import com.webstore.commerce.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    @Transactional
    public void createUser(CreateUserRequest dto){
        if (userRepository.existsByEmail(dto.email())){
            throw new BadCredentialsException("User with this email already exists");
        }
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest dto) {
        User user = userRepository.findByEmail(dto.email()).filter(
                u -> passwordEncoder.matches(dto.password(), u.getPassword()))
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password")
            );

        return jwtService.generateToken(user);
    }
}
