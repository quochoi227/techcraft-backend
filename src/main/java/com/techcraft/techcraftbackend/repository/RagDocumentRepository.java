package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.RagDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RagDocumentRepository extends JpaRepository<RagDocument, UUID> {
    List<RagDocument> findAllByOrderByCreatedAtDesc();
    boolean existsByTitleIgnoreCase(String title);
}
