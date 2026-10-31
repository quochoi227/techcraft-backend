package com.techcraft.techcraftbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rag_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "source_file", nullable = false)
    private String sourceFile;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "chunk_count", nullable = false)
    @Builder.Default
    private Integer chunkCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RagChunk> chunks = new ArrayList<>();

    public void addChunk(RagChunk chunk) {
        chunks.add(chunk);
        chunk.setDocument(this);
    }

    public void removeChunk(RagChunk chunk) {
        chunks.remove(chunk);
        chunk.setDocument(null);
    }
}
