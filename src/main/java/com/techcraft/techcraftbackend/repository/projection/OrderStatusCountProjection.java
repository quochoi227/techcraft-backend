package com.techcraft.techcraftbackend.repository.projection;

import com.techcraft.techcraftbackend.enums.OrderStatus;

public interface OrderStatusCountProjection {

    OrderStatus getStatus();

    Long getCount();
}
