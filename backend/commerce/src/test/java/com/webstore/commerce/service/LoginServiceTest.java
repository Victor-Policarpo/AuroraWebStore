package com.webstore.commerce.service;

import com.webstore.commerce.dto.request.CreateUserRequest;
import com.webstore.commerce.dto.request.LoginRequest;
import com.webstore.commerce.dto.response.LoginResponse;
import com.webstore.commerce.entity.enums.Role;
import com.webstore.commerce.entity.User;
import com.webstore.commerce.exception.errors.BadCredentialsException;
import com.webstore.commerce.exception.errors.DisabledException;
import com.webstore.commerce.mapper.UserMapper;
import com.webstore.commerce.repository.UserRepository;
import com.webstore.commerce.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private LoginService loginService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);

        loginService = new LoginService(
                userRepository,
                userMapper,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    @DisplayName("Should create user successfully when everything is OK")
    void createUser() {
        CreateUserRequest dto = new CreateUserRequest("John Doe","john@example.com", "password123");
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("encodedPassword");
        loginService.createUser(dto);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("John Doe", savedUser.getName());
        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(Role.CUSTOMER, savedUser.getRole());
    }

    @Test
    @DisplayName("Should throw exception when user with email already exists")
    void createUserThrowsExceptionWhenEmailExists() {
        CreateUserRequest dto = new CreateUserRequest("John Doe","john@example.com", "password123");
        when(userRepository.existsByEmail(dto.email())).thenReturn(true);
        assertThrows(BadCredentialsException.class, () -> loginService.createUser(dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully when everything is OK")
    void login() {
        LoginRequest dto = new LoginRequest("john@example.com", "password123");
        User user = setUser();
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), user.getPassword())).thenReturn(true);
        LoginResponse expectedResponse = new LoginResponse("token", 3600);
        when(jwtService.generateToken(user)).thenReturn(expectedResponse);
        LoginResponse response = loginService.login(dto);
        verify(jwtService).generateToken(user);
        assertEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Should throw exception when user is inactive")
    void loginThrowsExceptionWhenUserIsInactive() {
        LoginRequest dto = new LoginRequest("john@example.com", "password123");
        User user = setUser();
        user.setActive(false);
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), user.getPassword())).thenReturn(true);
        assertThrows(DisabledException.class, () -> loginService.login(dto));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email is invalid")
    void loginThrowsExceptionWhenEmailIsInvalid() {
        LoginRequest dto = new LoginRequest("john123@example.com", "password123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> loginService.login(dto));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when password is invalid")
    void loginThrowsExceptionWhenPasswordIsInvalid() {
        LoginRequest dto = new LoginRequest("john@example.com", "password321");
        User user = setUser();
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), user.getPassword())).thenReturn(false);
        assertThrows(BadCredentialsException.class, () -> loginService.login(dto));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    User setUser(){
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        return user;
    }

}