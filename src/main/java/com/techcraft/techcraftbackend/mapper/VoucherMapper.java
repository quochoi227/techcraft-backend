package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.request.CreateVoucherRequest;
import com.techcraft.techcraftbackend.dto.response.VoucherResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherUsageSummaryResponse;
import com.techcraft.techcraftbackend.entity.Voucher;
import com.techcraft.techcraftbackend.entity.VoucherUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface VoucherMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usedCount", constant = "0")
    @Mapping(target = "active", source = "isActive")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Voucher toEntity(CreateVoucherRequest request);

    @Mapping(target = "remainingUses", expression = "java(calculateRemainingUses(voucher))")
    @Mapping(target = "valid", expression = "java(calculateIsValid(voucher))")
    VoucherResponse toResponse(Voucher voucher);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    VoucherUsageSummaryResponse toUsageSummaryResponse(VoucherUsage usage);

    default Integer calculateRemainingUses(Voucher voucher) {
        if (voucher == null || voucher.getUsageLimit() == null) {
            return null;
        }
        int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        return Math.max(0, voucher.getUsageLimit() - used);
    }

    default boolean calculateIsValid(Voucher voucher) {
        if (voucher == null || !voucher.isActive()) {
            return false;
        }
        Instant now = Instant.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            return false;
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            return false;
        }
        if (voucher.getUsageLimit() != null) {
            int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
            if (used >= voucher.getUsageLimit()) {
                return false;
            }
        }
        return true;
    }
}
