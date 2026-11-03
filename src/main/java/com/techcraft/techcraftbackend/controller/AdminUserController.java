package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.request.AdminUpdateUserRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateUserStatusRequest;
import com.techcraft.techcraftbackend.dto.request.UserFilterRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.UserResponse;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @Valid @ModelAttribute UserFilterRequest filter) {
        PageResponse<UserResponse> response = userService.getUsers(filter);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công!", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công!", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> adminUpdateUser(
            Principal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        UserResponse response = userService.adminUpdateUser(principal.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin người dùng thành công!", response));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            Principal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        UserResponse response = userService.updateUserStatus(principal.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái người dùng thành công!", response));
    }
}
