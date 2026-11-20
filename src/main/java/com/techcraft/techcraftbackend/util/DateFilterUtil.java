package com.techcraft.techcraftbackend.util;

import com.techcraft.techcraftbackend.exception.BadRequestException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public final class DateFilterUtil {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private DateFilterUtil() {
    }

    public static Instant parseDateTime(String value, boolean isEndOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();

        // 1. Try Instant (ISO-8601 with timezone: 2026-11-01T00:00:00Z)
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }

        // 2. Try LocalDate (2026-11-01)
        try {
            LocalDate localDate = LocalDate.parse(trimmed);
            return isEndOfDay
                    ? localDate.atTime(LocalTime.MAX).atZone(DEFAULT_ZONE).toInstant()
                    : localDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        // 3. Try LocalDateTime (2026-11-01T15:30:00)
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed);
            return localDateTime.atZone(DEFAULT_ZONE).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        throw new BadRequestException("Định dạng thời gian không hợp lệ: '" + value +
                "'. Vui lòng dùng định dạng YYYY-MM-DD hoặc ISO-8601 (vd: 2026-11-01 hoặc 2026-11-01T00:00:00Z).");
    }
}
