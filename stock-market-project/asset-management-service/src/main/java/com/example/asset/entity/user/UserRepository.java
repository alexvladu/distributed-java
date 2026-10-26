package com.example.asset.entity.user;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(long id);

    boolean existsByUsername(String username);

    Optional<User> update(long id, String username, String email);

    void delete(long id);
}
