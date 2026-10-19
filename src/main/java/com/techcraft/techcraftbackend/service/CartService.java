package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.request.AddCartPcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {

    CartItemResponse addCartItem(UUID userId, AddCartItemRequest request);

    CartItemResponse addCartItem(String email, AddCartItemRequest request);

    CartItemResponse addPcCaseToCart(UUID userId, AddCartPcCaseRequest request);

    CartItemResponse addPcCaseToCart(String email, AddCartPcCaseRequest request);

    CartResponse getCart(UUID userId);

    CartResponse getCart(String email);
}
