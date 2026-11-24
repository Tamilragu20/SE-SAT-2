package com.lab.booking.service;

import com.lab.booking.exception.BusinessRuleException;
import com.lab.booking.model.User;
import com.lab.booking.model.UserStatus;
import com.lab.booking.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new BusinessRuleException("Email and password must not be empty.");
        }

        Optional<User> optionalUser = userRepository.findByEmail(email.trim().toLowerCase());
        if (optionalUser.isEmpty()) {
            throw new BusinessRuleException("Invalid email or password.");
        }

        User user = optionalUser.get();
        if (!user.getPassword().equals(password.trim())) {
            throw new BusinessRuleException("Invalid email or password.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Your account is currently inactive. Please contact administrator.");
        }

        return user;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
