package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.RegisterResponse;
import com.techcraft.techcraftbackend.dto.response.UserSummaryResponse;
import com.techcraft.techcraftbackend.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    RegisterResponse toRegisterResponse(User user);

    UserSummaryResponse toUserSummaryResponse(User user);
}
