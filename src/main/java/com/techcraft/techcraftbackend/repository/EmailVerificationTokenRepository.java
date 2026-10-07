package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.EmailVerificationToken;
import com.techcraft.techcraftbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findTopByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}
