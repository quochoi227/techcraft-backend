package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.RefreshToken;
import com.techcraft.techcraftbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    void deleteByUser(User user);
}
