package com.techcraft.techcraftbackend.entity;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "rag_chunks",
        indexes = {
                @Index(name = "idx_rag_chunks_document_id", columnList = "document_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private RagDocument document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(name = "embedding", columnDefinition = "vector(3072)", nullable = false)
    private PGvector embedding;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public float[] getEmbeddingArray() {
        return embedding != null ? embedding.toArray() : null;
    }

    public void setEmbeddingArray(float[] values) {
        this.embedding = values != null ? new PGvector(values) : null;
    }
}
