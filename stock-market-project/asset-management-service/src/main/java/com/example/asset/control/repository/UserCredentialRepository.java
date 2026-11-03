package com.example.asset.control.repository;

import java.util.Optional;

import com.example.asset.entity.auth.Credential;
import com.example.asset.entity.user.User;

import jakarta.ejb.Local;

@Local
public interface UserCredentialRepository {
    /** Creates the user and its credentials atomically. */
    User createAccount(String username, String email, String passwordHash);

    Optional<Credential> findByUsername(String username);

    void recordFailedAttempt(long userId);

    void recordSuccessfulLogin(long userId);
}
