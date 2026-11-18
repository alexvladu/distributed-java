package com.example.asset.control.repository;

import java.util.List;
import java.util.Optional;

import com.example.asset.entity.auth.Credential;
import com.example.asset.entity.auth.CredentialEntity;
import com.example.asset.entity.user.User;
import com.example.asset.entity.user.UserEntity;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

/** Credentials live in PostgreSQL next to the users table, so both are written in one transaction. */
@Stateless
public class JpaUserCredentialRepository implements UserCredentialRepository {
    @PersistenceContext(unitName = "postgres")
    private EntityManager entityManager;

    @Override
    public User createAccount(String username, String email, String passwordHash) {
        UserEntity user = new UserEntity(username, email);
        try {
            entityManager.persist(user);
            // Flushing assigns user_id, which the credentials row needs.
            entityManager.flush();
            entityManager.persist(new CredentialEntity(user.getUserId(), passwordHash));
            entityManager.flush();
        } catch (PersistenceException exception) {
            if (PersistenceErrors.isUniqueViolation(exception)) {
                throw new IllegalArgumentException("Username or email is already registered");
            }
            throw exception;
        }
        // Reads created_at, which PostgreSQL filled in.
        entityManager.refresh(user);
        return user.toUser();
    }

    @Override
    public Optional<Credential> findByUsername(String username) {
        List<Object[]> rows = entityManager.createQuery("""
                        SELECT c, u FROM CredentialEntity c, UserEntity u
                        WHERE c.userId = u.userId AND LOWER(u.username) = LOWER(:username)
                        ORDER BY u.userId""", Object[].class)
                .setParameter("username", username)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        CredentialEntity credential = (CredentialEntity) rows.get(0)[0];
        UserEntity user = (UserEntity) rows.get(0)[1];
        return Optional.of(credential.toCredential(user.getUsername()));
    }

    /** The row lock (SELECT ... FOR UPDATE) keeps concurrent failures from losing a count. */
    @Override
    public void recordFailedAttempt(long userId) {
        CredentialEntity credential = entityManager.find(CredentialEntity.class, userId,
                LockModeType.PESSIMISTIC_WRITE);
        if (credential != null) {
            credential.recordFailedAttempt();
        }
    }

    @Override
    public void recordSuccessfulLogin(long userId) {
        CredentialEntity credential = entityManager.find(CredentialEntity.class, userId);
        if (credential != null) {
            credential.recordSuccessfulLogin();
        }
    }
}
