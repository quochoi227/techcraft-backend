package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.enums.ChatIntent;
import com.techcraft.techcraftbackend.service.IntentClassifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class IntentClassifierServiceImpl implements IntentClassifierService {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final List<String> POLICY_KEYWORDS = List.of(
            // Đổi trả & hoàn tiền
            "doi tra", "doi hang", "doi moi", "duoc doi", "co duoc doi", "doi khong", "doi",
            "tra hang", "tra lai", "hoan tien", "hoan tra", "1 doi 1",
            // Bảo hành & sửa chữa
            "bao hanh", "bao duong", "sua chua", "sua", "loi", "hu hong", "hong", "loi phan cung",
            // Giao hàng & vận chuyển
            "van chuyen", "giao hang", "giao", "ship", "freeship", "phi ship", "phi van chuyen", "nhan hang",
            // Thanh toán & trả góp
            "thanh toan", "tra gop", "quet the", "chuyen khoan", "tien mat", "cod", "dat coc", "tra truoc",
            // Thông tin & liên hệ
            "chinh sach", "quy dinh", "dieu kien", "cam ket", "hotline", "so dien thoai", "sdt", "dia chi",
            "cua hang", "o dau", "chi nhanh", "gio mo cua", "gio lam viec", "mo cua", "dong cua",
            // Hóa đơn & khiếu nại
            "hoa don", "vat", "chung tu", "khieu nai", "lien he", "ho tro",
            // Thời hạn & thời gian
            "bao lau", "may ngay", "bao nhieu ngay", "thoi han", "thoi gian bao hanh", "han bao hanh", "qua han"
    );

    private static final List<String> PC_BUILD_INTENT_KEYWORDS = List.of(
            "cau hinh", "build", "ngan sach", "trieu", "tr", "lap may", "tu van may",
            "tu van pc", "tu van case", "choi game", "render", "edit video", "photoshop",
            "autocad", "fo4", "lol", "valorant", "csgo", "genshin", "wukong", "case pc",
            "mua pc", "mua may", "dan may"
    );

    private static final List<String> GENERAL_GREETINGS = List.of(
            "xin chao", "chao", "hello", "hi", "hey", "alo", "co ai khong",
            "ban la ai", "bot la ai", "shop oi", "ad oi"
    );

    @Override
    public ChatIntent classify(String message) {
        if (message == null || message.isBlank()) {
            return ChatIntent.GENERAL_CHAT;
        }

        String normalized = removeDiacritics(message.toLowerCase().trim());

        boolean hasPolicy = containsAnyKeyword(normalized, POLICY_KEYWORDS);
        boolean hasPcBuild = containsAnyKeyword(normalized, PC_BUILD_INTENT_KEYWORDS);

        if (hasPolicy && hasPcBuild) {
            log.debug("Classified message as HYBRID: '{}'", message);
            return ChatIntent.HYBRID;
        }

        if (hasPolicy) {
            log.debug("Classified message as POLICY_INQUIRY: '{}'", message);
            return ChatIntent.POLICY_INQUIRY;
        }

        if (hasPcBuild) {
            log.debug("Classified message as PC_RECOMMENDATION: '{}'", message);
            return ChatIntent.PC_RECOMMENDATION;
        }

        // Kiểm tra xem có phải câu chào hỏi ngắn không
        if (containsAnyKeyword(normalized, GENERAL_GREETINGS) || normalized.length() < 15) {
            log.debug("Classified message as GENERAL_CHAT: '{}'", message);
            return ChatIntent.GENERAL_CHAT;
        }

        // Mặc định cho câu hỏi chưa rõ ràng là GENERAL_CHAT để hướng dẫn user
        log.debug("Fallback classification to GENERAL_CHAT for message: '{}'", message);
        return ChatIntent.GENERAL_CHAT;
    }

    private boolean containsAnyKeyword(String text, List<String> keywords) {
        for (String keyword : keywords) {
            String pattern = "(?i)(^|\\s|\\W)" + Pattern.quote(keyword) + "($|\\s|\\W)";
            if (Pattern.compile(pattern).matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private String removeDiacritics(String text) {
        String nfdNormalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(nfdNormalized).replaceAll("");
        return withoutDiacritics.replace('đ', 'd').replace('Đ', 'D');
    }
}
