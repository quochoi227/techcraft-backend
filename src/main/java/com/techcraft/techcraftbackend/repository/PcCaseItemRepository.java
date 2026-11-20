package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.PcCaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PcCaseItemRepository extends JpaRepository<PcCaseItem, UUID> {
}
