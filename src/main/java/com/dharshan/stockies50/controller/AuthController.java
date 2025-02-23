package com.dharshan.stockies50.controller;

import com.dharshan.stockies50.model.User;
import com.dharshan.stockies50.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    // Render the sign-up form
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }
    
    // Process sign-up form submission
    @PostMapping("/signup")
    public String processSignup(@ModelAttribute("user") User user, Model model) {
        String result = customUserDetailsService.registerUser(user);
        if (!"SUCCESS".equals(result)) {
            model.addAttribute("errorMessage", result);
            return "signup";
        }
        return "redirect:/login";
    }
    
    // Render the login form
    @GetMapping("/login")
    public String loginForm(Model model) {
        return "login";
    }
}