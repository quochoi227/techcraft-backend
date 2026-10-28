package com.techcraft.techcraftbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private UUID productId;
    private String productName;
    private UUID pcCaseId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}
