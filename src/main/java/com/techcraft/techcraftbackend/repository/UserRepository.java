package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    long countByActiveTrue();

    long countByEmailVerifiedTrue();

    @Query("SELECT u.createdAt FROM User u " +
           "WHERE u.createdAt >= :startDate AND u.createdAt <= :endDate " +
           "ORDER BY u.createdAt ASC")
    List<Instant> findUserCreatedAtBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT u.createdAt FROM User u ORDER BY u.createdAt ASC")
    List<Instant> findAllUserCreatedAt();
}
