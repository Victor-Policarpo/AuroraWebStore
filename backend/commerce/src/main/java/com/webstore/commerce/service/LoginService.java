package com.webstore.commerce.service;

import com.webstore.commerce.dto.request.CreateUserRequest;
import com.webstore.commerce.entity.Role;
import com.webstore.commerce.entity.User;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.mapper.UserMapper;
import com.webstore.commerce.repository.UserRepository;
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

    @Transactional
    public void createUser(CreateUserRequest dto){
        if (userRepository.existsByEmail(dto.email())){
            throw new ResourceAlreadyExistsException("Email already exists");
        }
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);
    }
}
