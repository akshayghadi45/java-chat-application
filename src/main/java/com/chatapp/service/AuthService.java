package com.chatapp.service;
import com.chatapp.model.User;
import com.chatapp.model.dto.AuthRequest;
import com.chatapp.model.dto.AuthResponse;
import com.chatapp.model.dto.UserDTO;
import com.chatapp.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }
    public AuthResponse register(AuthRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Username already exists")
                    .build();
        }
        User user = userService.createUser(request.getUsername(), request.getPassword());
        String token = jwtTokenProvider.createToken(user.getUsername(), user.getId());
        return AuthResponse.builder()
                .success(true)
                .message("User registered successfully")
                .user(new UserDTO(user.getId(), user.getUsername(), user.isOnline()))
                .token(token)
                .build();
    }
    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            User user = userService.getUserByUsername(request.getUsername()).orElseThrow();
            user = userService.updateUserOnlineStatus(user.getId(), true);

            String token = jwtTokenProvider.createToken(user.getUsername(), user.getId());
            return AuthResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .user(new UserDTO(user.getId(), user.getUsername(), user.isOnline()))
                    .token(token)
                    .build();
        } catch (AuthenticationException e) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid username/password")
                    .build();
        }
    }
}