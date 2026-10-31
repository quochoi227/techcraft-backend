package com.techcraft.techcraftbackend.service.impl;

import com.pgvector.PGvector;
import com.techcraft.techcraftbackend.dto.response.RagDocumentResponse;
import com.techcraft.techcraftbackend.entity.RagDocument;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.repository.RagChunkRepository;
import com.techcraft.techcraftbackend.repository.RagDocumentRepository;
import com.techcraft.techcraftbackend.repository.projection.ChunkSimilarityProjection;
import com.techcraft.techcraftbackend.service.DocumentParserService;
import com.techcraft.techcraftbackend.service.GeminiService;
import com.techcraft.techcraftbackend.service.RagService;
import com.techcraft.techcraftbackend.service.TextChunkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final RagDocumentRepository ragDocumentRepository;
    private final RagChunkRepository ragChunkRepository;
    private final DocumentParserService documentParserService;
    private final TextChunkerService textChunkerService;
    private final GeminiService geminiService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public RagDocumentResponse uploadAndIndexDocument(MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tập tin tải lên không được để trống.");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String docTitle = (title != null && !title.isBlank()) ? title.trim() : extractDefaultTitle(originalFilename);

        if (ragDocumentRepository.existsByTitleIgnoreCase(docTitle)) {
            throw new DuplicateResourceException("Tài liệu với tiêu đề '" + docTitle + "' đã tồn tại trong hệ thống.");
        }

        // 1. Trích xuất text từ file
        String parsedText = documentParserService.parse(file);
        if (parsedText == null || parsedText.isBlank()) {
            throw new BadRequestException("Không thể trích xuất nội dung văn bản từ tập tin.");
        }

        // 2. Chia text thành chunks
        List<String> textChunks = textChunkerService.chunk(parsedText);
        if (textChunks.isEmpty()) {
            throw new BadRequestException("Nội dung tài liệu quá ngắn hoặc không thể tạo chunks.");
        }

        // 3. Tạo Document entity và flush ngay vào DB trước khi insert chunks qua JDBC
        RagDocument document = RagDocument.builder()
                .title(docTitle)
                .sourceFile(originalFilename)
                .fileSize(file.getSize())
                .chunkCount(textChunks.size())
                .build();

        RagDocument savedDoc = ragDocumentRepository.saveAndFlush(document);

        // 4. Tạo vector embeddings và lưu Chunks bằng JdbcTemplate với CAST(? AS vector)
        String insertSql = "INSERT INTO rag_chunks (id, document_id, chunk_index, content, embedding, created_at) " +
                "VALUES (?, ?, ?, ?, cast(? as vector), ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < textChunks.size(); i++) {
            String chunkContent = textChunks.get(i);
            float[] embeddingArray = geminiService.generateEmbedding(chunkContent);
            String vectorString = new PGvector(embeddingArray).getValue();
            UUID chunkId = UUID.randomUUID();

            batchArgs.add(new Object[]{
                    chunkId,
                    savedDoc.getId(),
                    i,
                    chunkContent,
                    vectorString,
                    Timestamp.from(now)
            });
        }

        jdbcTemplate.batchUpdate(insertSql, batchArgs);
        log.info("Lập chỉ mục RAG thành công cho tài liệu '{}' ({} chunks)", docTitle, batchArgs.size());

        return toDocumentResponse(savedDoc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RagDocumentResponse> getAllDocuments() {
        return ragDocumentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id) {
        RagDocument document = ragDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu RAG với ID: " + id));

        ragChunkRepository.deleteByDocumentId(document.getId());
        ragDocumentRepository.delete(document);
        log.info("Đã xóa tài liệu RAG ID '{}' và các chunks liên quan.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChunkSimilarityProjection> searchSimilarChunks(String queryText, int topK, double minSimilarity) {
        if (queryText == null || queryText.isBlank()) {
            return Collections.emptyList();
        }

        try {
            float[] queryEmbedding = geminiService.generateEmbedding(queryText);
            String vectorString = new PGvector(queryEmbedding).getValue();
            return ragChunkRepository.findSimilarChunks(vectorString, minSimilarity, topK);
        } catch (Exception e) {
            log.error("Lỗi khi tìm kiếm vector tương đồng: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String extractDefaultTitle(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    private RagDocumentResponse toDocumentResponse(RagDocument doc) {
        return RagDocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .sourceFile(doc.getSourceFile())
                .fileSize(doc.getFileSize())
                .chunkCount(doc.getChunkCount())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
