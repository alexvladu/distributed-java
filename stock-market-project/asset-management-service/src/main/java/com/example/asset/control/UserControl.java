package com.example.asset.control;

import com.example.asset.entity.user.User;
import com.example.asset.entity.user.UserRepository;
import java.util.List;

public final class UserControl {
    private final UserRepository repository;
    public UserControl(UserRepository repository) { this.repository = repository; }
    public List<User> findAll() { return repository.findAll(); }
    public User find(long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id)); }
    public User create(String username, String email) {
        require(username, "username"); require(email, "email");
        return repository.create(username.trim(), email.trim());
    }
    public User update(long userId, long id, String username, String email) {
        if (userId != id) throw new SecurityException("Users can update only their own profile");
        require(username, "username"); require(email, "email");
        return repository.update(id, username.trim(), email.trim());
    }
    public void delete(long userId, long id) {
        if (userId != id) throw new SecurityException("Users can delete only their own profile");
        repository.delete(id);
    }
    private void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }
}
