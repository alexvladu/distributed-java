package com.example.asset.entity.user;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** PostgreSQL {@code users} row (persistence unit {@code postgres}). */
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    /** Filled in by PostgreSQL ({@code DEFAULT CURRENT_TIMESTAMP}). */
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected UserEntity() {
    }

    public UserEntity(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void update(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public User toUser() {
        return new User(userId, username, email, createdAt);
    }
}
