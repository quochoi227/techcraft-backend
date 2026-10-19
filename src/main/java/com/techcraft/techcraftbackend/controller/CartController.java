package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.request.AddCartPcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.dto.response.CartResponse;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        CartResponse response = cartService.getCart(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giỏ hàng thành công!", response));
    }

    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartItemResponse>> addCartItem(
            Principal principal,
            @Valid @RequestBody AddCartItemRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        CartItemResponse response = cartService.addCartItem(principal.getName(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm linh kiện vào giỏ hàng thành công!", response));
    }

    @PostMapping("/pc-cases")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartItemResponse>> addPcCaseToCart(
            Principal principal,
            @Valid @RequestBody AddCartPcCaseRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        CartItemResponse response = cartService.addPcCaseToCart(principal.getName(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm cấu hình PC vào giỏ hàng thành công!", response));
    }
}
