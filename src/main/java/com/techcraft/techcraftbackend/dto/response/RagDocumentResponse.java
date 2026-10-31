package com.techcraft.techcraftbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocumentResponse {

    private UUID id;
    private String title;
    private String sourceFile;
    private Long fileSize;
    private Integer chunkCount;
    private Instant createdAt;
    private Instant updatedAt;
}
