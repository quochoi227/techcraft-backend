package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.request.ChatRequest;
import com.techcraft.techcraftbackend.dto.response.ChatResponse;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.enums.ChatIntent;
import com.techcraft.techcraftbackend.repository.projection.ChunkSimilarityProjection;
import com.techcraft.techcraftbackend.service.ChatService;
import com.techcraft.techcraftbackend.service.GeminiService;
import com.techcraft.techcraftbackend.service.IntentClassifierService;
import com.techcraft.techcraftbackend.service.PcConfigSuggestionService;
import com.techcraft.techcraftbackend.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final IntentClassifierService intentClassifierService;
    private final RagService ragService;
    private final PcConfigSuggestionService pcConfigSuggestionService;
    private final GeminiService geminiService;

    @Value("${pgvector.top-k:4}")
    private int topK;

    @Value("${pgvector.min-similarity:0.55}")
    private double minSimilarity;

    @Value("${app.admin.phone:0123456789}")
    private String storePhone;

    @Value("${app.admin.email:admin@techcraft.com}")
    private String storeEmail;

    @Override
    public ChatResponse chat(ChatRequest request) {
        String message = request.getMessage().trim();
        ChatIntent intent = intentClassifierService.classify(message);
        log.info("Phân loại ý định tin nhắn: '{}' -> {}", message, intent);

        List<ChunkSimilarityProjection> policyChunks = Collections.emptyList();
        String pcCasesPromptText = "";
        List<String> policySources = new ArrayList<>();

        // 1. Lấy context phù hợp theo intent
        switch (intent) {
            case POLICY_INQUIRY -> {
                policyChunks = ragService.searchSimilarChunks(message, topK, minSimilarity);
                policySources = extractSources(policyChunks);
            }
            case PC_RECOMMENDATION -> {
                pcCasesPromptText = pcConfigSuggestionService.getAvailablePcCasesPromptText();
            }
            case HYBRID -> {
                policyChunks = ragService.searchSimilarChunks(message, topK, minSimilarity);
                policySources = extractSources(policyChunks);
                pcCasesPromptText = pcConfigSuggestionService.getAvailablePcCasesPromptText();
            }
            case GENERAL_CHAT -> {
                // Không query database, giảm thiểu token và latency
            }
        }

        // 2. Xây dựng prompt system instruction
        String systemInstruction = buildSystemInstruction(intent, policyChunks, pcCasesPromptText);

        // 3. Gọi Gemini API
        String rawReply = geminiService.generateText(systemInstruction, request.getHistory(), message);

        // 4. Trích xuất mã PC Case nếu có gợi ý
        Set<UUID> suggestedCaseIds = pcConfigSuggestionService.extractPcCaseIds(rawReply);
        List<PcCaseResponse> suggestedPcCases = pcConfigSuggestionService.findFullPcCasesByIds(suggestedCaseIds);

        // 5. Làm sạch thẻ [PC_CASE_ID: ...] trong nội dung phản hồi hiển thị
        String cleanedReply = rawReply.replaceAll("\\[PC_CASE_ID:\\s*[0-9a-fA-F\\-]{36}\\]", "").trim();

        return ChatResponse.builder()
                .reply(cleanedReply)
                .suggestedPcCases(suggestedPcCases)
                .policySources(policySources)
                .detectedIntent(intent)
                .timestamp(Instant.now())
                .build();
    }

    private List<String> extractSources(List<ChunkSimilarityProjection> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }
        return chunks.stream()
                .map(ChunkSimilarityProjection::getDocumentTitle)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String buildSystemInstruction(ChatIntent intent, List<ChunkSimilarityProjection> policyChunks, String pcCasesPromptText) {
        StringBuilder sb = new StringBuilder();

        sb.append("Bạn là TechCraft AI Assistant — trợ lý tư vấn thông minh của cửa hàng linh kiện máy tính TechCraft.\n");
        sb.append("Nhiệm vụ của bạn là hỗ trợ khách hàng giải đáp chính sách của cửa hàng và tư vấn cấu hình PC theo nhu cầu & ngân sách.\n");
        sb.append("Thông tin liên hệ cửa hàng TechCraft: Hotline/SĐT: ").append(storePhone).append(", Email: ").append(storeEmail).append(".\n");
        sb.append("Phong cách trả lời: Thân thiện, tận tâm, trung thực, chuyên nghiệp, sử dụng tiếng Việt tự nhiên và định dạng Markdown rõ ràng.\n\n");

        sb.append("=== NGUYÊN TẮC CỐT LÕI (BẮT BUỘC TUÂN THỦ NGHIÊM NGẶT) ===\n");
        sb.append("1. VỀ CHÍNH SÁCH CỬA HÀNG (đổi trả, bảo hành, vận chuyển, giao hàng, thanh toán...):\n");
        sb.append("   - Bạn CHỈ ĐƯỢC PHÉP TRẢ LỜI DỰA TRÊN TÀI LIỆU CHÍNH SÁCH ĐƯỢC CUNG CẤP DƯỚI ĐÂY.\n");
        sb.append("   - TUYỆT ĐỐI KHÔNG TỰ BỊA ĐẶT, suy đoán hoặc đưa ra bất kỳ con số ngày (ví dụ: 7 ngày, 15 ngày, 30 ngày...), quy định hay điều kiện đổi trả/bảo hành nào nếu không có tài liệu bằng chứng được cung cấp trong prompt.\n");
        sb.append("   - Nếu KHÔNG CÓ tài liệu chính sách được cung cấp hoặc tài liệu không đề cập đến câu hỏi của khách hàng, bạn BẮT BUỘC phải thông báo lịch sự rằng hiện tại hệ thống AI của cửa hàng chưa có thông tin chính sách chính thức về vấn đề này, và hướng dẫn khách hàng liên hệ trực tiếp hotline (").append(storePhone).append(") hoặc đến trực tiếp cửa hàng để được nhân viên hỗ trợ chi tiết.\n");
        sb.append("2. VỀ GỢI Ý CẤU HÌNH PC:\n");
        sb.append("   - BẮT BUỘC chỉ được chọn và gợi ý từ danh sách cấu hình PC Case có sẵn ở dưới. TUYỆT ĐỐI KHÔNG tự lắp ghép hoặc bịa thêm linh kiện ngoài danh sách.\n");
        sb.append("   - Khi gợi ý bất kỳ cấu hình nào, BẮT BUỘC phải đính kèm thẻ nhận diện theo đúng định dạng: [PC_CASE_ID: <mã id>] ngay cạnh tên cấu hình để hệ thống tự động hiển thị thẻ sản phẩm cho khách hàng.\n\n");

        if (!policyChunks.isEmpty()) {
            sb.append("=== TÀI LIỆU CHÍNH SÁCH CỬA HÀNG ĐƯỢC CUNG CẤP ===\n");
            for (ChunkSimilarityProjection chunk : policyChunks) {
                sb.append("--- [Tài liệu: ").append(chunk.getDocumentTitle()).append("] ---\n");
                sb.append(chunk.getContent()).append("\n\n");
            }
            sb.append("QUY TẮC: Trả lời chính xác, trung thực theo đúng nội dung tài liệu trên. Không thêm thắt điều khoản không có trong tài liệu.\n\n");
        } else if (intent == ChatIntent.POLICY_INQUIRY || intent == ChatIntent.HYBRID) {
            sb.append("=== TÌNH TRẠNG TÀI LIỆU CHÍNH SÁCH ===\n");
            sb.append("Hệ thống xác nhận: KHÔNG CÓ bất kỳ tài liệu chính sách nào liên quan đến câu hỏi này được lưu trong hệ thống.\n");
            sb.append("YÊU CẦU BẮT BUỘC: Bạn phải trả lời thẳng thắn và lịch sự rằng hiện tại hệ thống cửa hàng chưa cập nhật thông tin chính sách chính thức về vấn đề này, và mời khách hàng liên hệ hotline ").append(storePhone).append(" để được nhân viên hỗ trợ chi tiết. TUYỆT ĐỐI KHÔNG tự phỏng đoán số ngày đổi trả hay điều kiện.\n\n");
        }

        if (!pcCasesPromptText.isBlank()) {
            sb.append("=== ").append(pcCasesPromptText).append("\n\n");
            sb.append("Phân tích ưu nhược điểm, độ phù hợp với ngân sách và nhu cầu của khách hàng một cách khách quan.\n\n");
        }

        if (intent == ChatIntent.GENERAL_CHAT) {
            sb.append("QUY TẮC CHÀO HỎI:\n");
            sb.append("Khách hàng đang chào hỏi hoặc mở đầu cuộc trò chuyện. Hãy chào lại vui vẻ, giới thiệu ngắn gọn bạn có thể giúp tư vấn cấu hình PC hoặc giải đáp các chính sách của TechCraft (bảo hành, đổi trả, giao hàng,...).\n");
        }

        return sb.toString();
    }
}
