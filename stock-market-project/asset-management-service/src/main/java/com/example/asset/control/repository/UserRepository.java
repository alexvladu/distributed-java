package com.example.asset.control.repository;

import java.util.Optional;

import com.example.asset.entity.user.User;

import jakarta.ejb.Local;

@Local
public interface UserRepository {
    Optional<User> findById(long id);

    boolean existsByUsername(String username);

    Optional<User> update(long id, String username, String email);

    void delete(long id);
}
