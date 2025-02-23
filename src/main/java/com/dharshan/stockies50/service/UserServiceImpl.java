package com.dharshan.stockies50.service;

import com.dharshan.stockies50.model.User;
import com.dharshan.stockies50.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public String saveUser(User user) {
        // Check if the email is already used
        Optional<User> existing = userRepository.findByEmail(user.getEmail());
        if(existing.isPresent()) {
            return "Email already exists!";
        }
        // Check if the passwords match
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            return "Password and Confirm Password do not match!";
        }
        userRepository.save(user);
        return "SUCCESS";
    }
}