package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.TimePeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponse {

    private Long totalUsers;
    private Long newUsersInPeriod;
    private Long activeUsers;
    private Long verifiedUsers;
    private TimePeriod period;
    private List<UserTimelinePointResponse> timeline;
}
