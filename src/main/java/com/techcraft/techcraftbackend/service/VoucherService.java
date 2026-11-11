package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.AdminVoucherFilterRequest;
import com.techcraft.techcraftbackend.dto.request.CreateVoucherRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateVoucherRequest;
import com.techcraft.techcraftbackend.dto.request.VoucherFilterRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.ValidateVoucherResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherDetailResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherResponse;
import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.entity.Voucher;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface VoucherService {

    VoucherResponse createVoucher(CreateVoucherRequest request);

    VoucherResponse updateVoucher(UUID id, UpdateVoucherRequest request);

    void deleteVoucher(UUID id);

    VoucherResponse toggleVoucherStatus(UUID id);

    PageResponse<VoucherResponse> getAllVouchers(AdminVoucherFilterRequest filter);

    VoucherDetailResponse getVoucherDetail(UUID id);

    PageResponse<VoucherResponse> getPublicVouchers(VoucherFilterRequest filter);

    List<VoucherResponse> getAvailableVouchers();

    VoucherResponse getVoucherByCode(String code);

    ValidateVoucherResponse validateVoucher(String code, BigDecimal orderAmount, String userEmail);

    Voucher applyVoucherToOrder(String voucherCode, BigDecimal subtotalAmount, User user, Order order);

    void restoreVoucherUsage(Order order);
}
