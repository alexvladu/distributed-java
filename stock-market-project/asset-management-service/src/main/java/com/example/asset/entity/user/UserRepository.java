package com.example.asset.entity.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User create(String username, String email);
    Optional<User> findById(long id);
    List<User> findAll();
    User update(long id, String username, String email);
    void delete(long id);
}
