package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.PasswordResetToken;
import com.techcraft.techcraftbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findAllByUserAndUsedFalse(User user);

    void deleteByUser(User user);
}
