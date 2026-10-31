package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.RagChunk;
import com.techcraft.techcraftbackend.repository.projection.ChunkSimilarityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RagChunkRepository extends JpaRepository<RagChunk, UUID> {

    @Modifying
    @Query(value = "DELETE FROM rag_chunks WHERE document_id = :documentId", nativeQuery = true)
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    @Query(value = """
            SELECT c.id AS id,
                   c.document_id AS documentId,
                   c.chunk_index AS chunkIndex,
                   c.content AS content,
                   (1 - (c.embedding <=> cast(:queryVector as vector))) AS similarity,
                   d.title AS documentTitle
            FROM rag_chunks c
            JOIN rag_documents d ON c.document_id = d.id
            WHERE (1 - (c.embedding <=> cast(:queryVector as vector))) >= :minSimilarity
            ORDER BY c.embedding <=> cast(:queryVector as vector) ASC
            LIMIT :topK
            """, nativeQuery = true)
    List<ChunkSimilarityProjection> findSimilarChunks(
            @Param("queryVector") String queryVector,
            @Param("minSimilarity") double minSimilarity,
            @Param("topK") int topK
    );
}
