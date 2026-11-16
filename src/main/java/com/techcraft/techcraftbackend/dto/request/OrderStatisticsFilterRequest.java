package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.exception.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsFilterRequest {

    private String startDate;
    private String endDate;

    public void setStart_date(String startDate) {
        this.startDate = startDate;
    }

    public void setEnd_date(String endDate) {
        this.endDate = endDate;
    }

    public Instant getParsedStartDate() {
        return parseDateTime(this.startDate, false);
    }

    public Instant getParsedEndDate() {
        return parseDateTime(this.endDate, true);
    }

    private Instant parseDateTime(String value, boolean isEndOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDate localDate = LocalDate.parse(trimmed);
            return isEndOfDay
                    ? localDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant()
                    : localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed);
            return localDateTime.atZone(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        throw new BadRequestException("Định dạng thời gian không hợp lệ: '" + value +
                "'. Vui lòng dùng định dạng YYYY-MM-DD hoặc ISO-8601 (vd: 2026-11-01 hoặc 2026-11-01T00:00:00Z).");
    }
}
