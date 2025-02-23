package com.dharshan.stockies50.service;

import com.dharshan.stockies50.model.User;

public interface UserService {
    /**
     * Saves the given user if validation passes.
     * @param user the user to save
     * @return "SUCCESS" if saved successfully, otherwise an error message.
     */
    String saveUser(User user);
}