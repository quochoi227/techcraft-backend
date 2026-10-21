package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.request.AddCartPcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateCartItemRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateCartPcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {

    CartItemResponse addCartItem(UUID userId, AddCartItemRequest request);

    CartItemResponse addCartItem(String email, AddCartItemRequest request);

    CartItemResponse updateCartItemQuantity(UUID userId, UUID itemId, UpdateCartItemRequest request);

    CartItemResponse updateCartItemQuantity(String email, UUID itemId, UpdateCartItemRequest request);

    void deleteCartItem(UUID userId, UUID itemId);

    void deleteCartItem(String email, UUID itemId);

    CartItemResponse addPcCaseToCart(UUID userId, AddCartPcCaseRequest request);

    CartItemResponse addPcCaseToCart(String email, AddCartPcCaseRequest request);

    CartItemResponse updateCartPcCaseQuantity(UUID userId, UUID itemId, UpdateCartPcCaseRequest request);

    CartItemResponse updateCartPcCaseQuantity(String email, UUID itemId, UpdateCartPcCaseRequest request);

    CartResponse getCart(UUID userId);

    CartResponse getCart(String email);
}
