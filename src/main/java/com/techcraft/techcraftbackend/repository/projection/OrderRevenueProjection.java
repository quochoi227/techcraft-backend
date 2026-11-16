package com.techcraft.techcraftbackend.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface OrderRevenueProjection {

    Instant getCreatedAt();

    BigDecimal getTotalAmount();
}
