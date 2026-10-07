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
public class ProductImageResponse {

    private UUID id;
    private String imageUrl;
    private boolean isPrimary;
    private Integer displayOrder;
    private Instant createdAt;
}
