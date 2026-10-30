package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.response.RagDocumentResponse;
import com.techcraft.techcraftbackend.repository.projection.ChunkSimilarityProjection;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface RagService {

    RagDocumentResponse uploadAndIndexDocument(MultipartFile file, String title);

    List<RagDocumentResponse> getAllDocuments();

    void deleteDocument(UUID id);

    List<ChunkSimilarityProjection> searchSimilarChunks(String queryText, int topK, double minSimilarity);
}
