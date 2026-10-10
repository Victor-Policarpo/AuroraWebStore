package com.webstore.commerce.service;

import com.webstore.commerce.entity.User;
import com.webstore.commerce.exception.errors.BadCredentialsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("User is not authenticated");
        }
        return UUID.fromString(authentication.getName());
    }

    public User getAuthenticatedUser() {
        UUID userId = getAuthenticatedUserId();
        return userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("Authenticated user not found in the database")
        );
    }

}
