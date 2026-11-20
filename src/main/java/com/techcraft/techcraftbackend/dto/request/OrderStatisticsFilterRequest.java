package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.util.DateFilterUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
        return DateFilterUtil.parseDateTime(this.startDate, false);
    }

    public Instant getParsedEndDate() {
        return DateFilterUtil.parseDateTime(this.endDate, true);
    }
}
