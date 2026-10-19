package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;

import java.util.UUID;

public interface CartService {

    CartItemResponse addCartItem(UUID userId, AddCartItemRequest request);

    CartItemResponse addCartItem(String email, AddCartItemRequest request);
}
