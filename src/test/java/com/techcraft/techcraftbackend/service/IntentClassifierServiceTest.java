package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.enums.ChatIntent;
import com.techcraft.techcraftbackend.service.impl.IntentClassifierServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentClassifierServiceTest {

    private IntentClassifierService intentClassifierService;

    @BeforeEach
    void setUp() {
        intentClassifierService = new IntentClassifierServiceImpl();
    }

    @Test
    void classify_PolicyInquiry_ShouldReturnPolicyInquiry() {
        assertEquals(ChatIntent.POLICY_INQUIRY,
                intentClassifierService.classify("Cửa hàng bảo hành VGA trong bao lâu?"));
        assertEquals(ChatIntent.POLICY_INQUIRY,
                intentClassifierService.classify("Chính sách đổi trả hàng như thế nào?"));
        assertEquals(ChatIntent.POLICY_INQUIRY,
                intentClassifierService.classify("Nếu dùng quá 7 ngày thì có được đổi không"));
        assertEquals(ChatIntent.POLICY_INQUIRY,
                intentClassifierService.classify("Shop có giao hàng COD hoặc freeship không?"));
        assertEquals(ChatIntent.POLICY_INQUIRY,
                intentClassifierService.classify("Địa chỉ cửa hàng ở đâu và hotline liên hệ?"));
    }

    @Test
    void classify_PcRecommendation_ShouldReturnPcRecommendation() {
        assertEquals(ChatIntent.PC_RECOMMENDATION,
                intentClassifierService.classify("Tư vấn cho mình cấu hình PC 20 triệu chơi game"));
        assertEquals(ChatIntent.PC_RECOMMENDATION,
                intentClassifierService.classify("Build máy tính làm đồ họa và render video"));
        assertEquals(ChatIntent.PC_RECOMMENDATION,
                intentClassifierService.classify("Cần mua một case PC tầm 15tr chơi Black Myth Wukong mượt"));
        assertEquals(ChatIntent.PC_RECOMMENDATION,
                intentClassifierService.classify("Gợi ý cấu hình máy tính văn phòng tiết kiệm"));
    }

    @Test
    void classify_Hybrid_ShouldReturnHybrid() {
        assertEquals(ChatIntent.HYBRID,
                intentClassifierService.classify("Tư vấn giúp mình PC gaming 20 triệu và shop có hỗ trợ trả góp qua thẻ tín dụng không?"));
        assertEquals(ChatIntent.HYBRID,
                intentClassifierService.classify("Cấu hình PC này có được freeship và bảo hành tận nơi không?"));
    }

    @Test
    void classify_GeneralChat_ShouldReturnGeneralChat() {
        assertEquals(ChatIntent.GENERAL_CHAT, intentClassifierService.classify("Xin chào shop"));
        assertEquals(ChatIntent.GENERAL_CHAT, intentClassifierService.classify("Hello"));
        assertEquals(ChatIntent.GENERAL_CHAT, intentClassifierService.classify("Hi bot"));
        assertEquals(ChatIntent.GENERAL_CHAT, intentClassifierService.classify(""));
        assertEquals(ChatIntent.GENERAL_CHAT, intentClassifierService.classify(null));
    }
}
