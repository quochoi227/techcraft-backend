package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.enums.PaymentMethod;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private String shippingAddress;

    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    private String note;

    private List<@Valid CreateOrderItemRequest> items;
}
