package com.chatapp.service;
import com.chatapp.model.User;
import com.chatapp.model.dto.UserDTO;
import com.chatapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public User createUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setOnline(true);
        user.setLastSeen(new Date());
        return userRepository.save(user);
    }
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDTO(user.getId(), user.getUsername(), user.isOnline()))
                .collect(Collectors.toList());
    }
    public User updateUserOnlineStatus(Long id, boolean isOnline) {
        return userRepository.findById(id).map(user -> {
            user.setOnline(isOnline);
            user.setLastSeen(new Date());
            return userRepository.save(user);
        }).orElse(null);
    }
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}