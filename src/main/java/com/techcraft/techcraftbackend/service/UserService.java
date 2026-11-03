package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.AdminUpdateUserRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateProfileRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateUserStatusRequest;
import com.techcraft.techcraftbackend.dto.request.UserFilterRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getProfile(String userEmail);

    UserResponse updateProfile(String userEmail, UpdateProfileRequest request);

    PageResponse<UserResponse> getUsers(UserFilterRequest filter);

    UserResponse getUserById(UUID id);

    UserResponse adminUpdateUser(String adminEmail, UUID id, AdminUpdateUserRequest request);

    UserResponse updateUserStatus(String adminEmail, UUID id, UpdateUserStatusRequest request);
}
