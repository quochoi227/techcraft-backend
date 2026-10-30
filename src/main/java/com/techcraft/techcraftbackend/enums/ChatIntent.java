package com.techcraft.techcraftbackend.enums;

public enum ChatIntent {
    POLICY_INQUIRY,      // Hỏi về chính sách, bảo hành, đổi trả, ship... (chỉ query RAG)
    PC_RECOMMENDATION,   // Hỏi về tư vấn, build cấu hình PC (chỉ query PC Cases)
    HYBRID,              // Kết hợp cả hai
    GENERAL_CHAT         // Chào hỏi xã giao (không query DB)
}
