package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.PcCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PcCaseRepository extends JpaRepository<PcCase, UUID>, JpaSpecificationExecutor<PcCase> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
