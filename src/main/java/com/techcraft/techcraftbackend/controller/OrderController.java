package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.request.CreateOrderRequest;
import com.techcraft.techcraftbackend.dto.response.OrderResponse;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            Principal principal,
            @Valid @RequestBody(required = false) CreateOrderRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        if (request == null) {
            request = new CreateOrderRequest();
        }

        OrderResponse response = orderService.createOrder(principal.getName(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đơn hàng thành công!", response));
    }
}
