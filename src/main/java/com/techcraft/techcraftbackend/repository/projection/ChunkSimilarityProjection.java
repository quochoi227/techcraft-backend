package com.techcraft.techcraftbackend.repository.projection;

import java.util.UUID;

public interface ChunkSimilarityProjection {
    UUID getId();
    UUID getDocumentId();
    Integer getChunkIndex();
    String getContent();
    Double getSimilarity();
    String getDocumentTitle();
}
