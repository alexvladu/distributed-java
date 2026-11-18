package com.example.asset.control.repository;

import java.util.Optional;

import com.example.asset.entity.user.User;
import com.example.asset.entity.user.UserEntity;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

/** Users live in PostgreSQL. Every method runs in its own container-managed transaction. */
@Stateless
public class JpaUserRepository implements UserRepository {
    @PersistenceContext(unitName = "postgres")
    private EntityManager entityManager;

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(entityManager.find(UserEntity.class, id)).map(UserEntity::toUser);
    }

    @Override
    public boolean existsByUsername(String username) {
        return entityManager.createQuery(
                        "SELECT COUNT(u) FROM UserEntity u WHERE LOWER(u.username) = LOWER(:username)", Long.class)
                .setParameter("username", username)
                .getSingleResult() > 0;
    }

    @Override
    public Optional<User> update(long id, String username, String email) {
        UserEntity user = entityManager.find(UserEntity.class, id);
        if (user == null) {
            return Optional.empty();
        }
        user.update(username, email);
        try {
            entityManager.flush();
        } catch (PersistenceException exception) {
            if (PersistenceErrors.isUniqueViolation(exception)) {
                throw new IllegalArgumentException("Username or email is already in use");
            }
            throw exception;
        }
        return Optional.of(user.toUser());
    }

    /** {@code user_credentials} rows are removed by the database ({@code ON DELETE CASCADE}). */
    @Override
    public void delete(long id) {
        UserEntity user = entityManager.find(UserEntity.class, id);
        if (user != null) {
            entityManager.remove(user);
        }
    }
}
