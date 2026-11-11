package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.request.AdminVoucherFilterRequest;
import com.techcraft.techcraftbackend.dto.request.CreateVoucherRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateVoucherRequest;
import com.techcraft.techcraftbackend.dto.request.VoucherFilterRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.ValidateVoucherResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherDetailResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherUsageSummaryResponse;
import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.entity.Voucher;
import com.techcraft.techcraftbackend.entity.VoucherUsage;
import com.techcraft.techcraftbackend.enums.DiscountType;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.VoucherMapper;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.repository.VoucherRepository;
import com.techcraft.techcraftbackend.repository.VoucherUsageRepository;
import com.techcraft.techcraftbackend.service.VoucherService;
import com.techcraft.techcraftbackend.specification.VoucherSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final UserRepository userRepository;
    private final VoucherMapper voucherMapper;

    @Override
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu yêu cầu không được để trống.");
        }

        String normalizedCode = request.getCode().trim().toUpperCase();

        if (voucherRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new BadRequestException("Mã giảm giá '" + normalizedCode + "' đã tồn tại trong hệ thống.");
        }

        validateVoucherParameters(
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMaxDiscountAmount(),
                request.getMinOrderAmount(),
                request.getStartDate(),
                request.getEndDate()
        );

        Voucher voucher = voucherMapper.toEntity(request);
        voucher.setCode(normalizedCode);
        voucher.setUsedCount(0);
        if (request.getIsActive() != null) {
            voucher.setActive(request.getIsActive());
        }

        Voucher savedVoucher = voucherRepository.save(voucher);
        return voucherMapper.toResponse(savedVoucher);
    }

    @Override
    public VoucherResponse updateVoucher(UUID id, UpdateVoucherRequest request) {
        if (id == null) {
            throw new BadRequestException("Mã định danh voucher không được để trống.");
        }

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));

        if (request == null) {
            return voucherMapper.toResponse(voucher);
        }

        DiscountType newType = request.getDiscountType() != null ? request.getDiscountType() : voucher.getDiscountType();
        BigDecimal newValue = request.getDiscountValue() != null ? request.getDiscountValue() : voucher.getDiscountValue();
        BigDecimal newMaxDiscount = request.getMaxDiscountAmount() != null ? request.getMaxDiscountAmount() : voucher.getMaxDiscountAmount();
        BigDecimal newMinOrder = request.getMinOrderAmount() != null ? request.getMinOrderAmount() : voucher.getMinOrderAmount();
        Instant newStartDate = request.getStartDate() != null ? request.getStartDate() : voucher.getStartDate();
        Instant newEndDate = request.getEndDate() != null ? request.getEndDate() : voucher.getEndDate();

        validateVoucherParameters(newType, newValue, newMaxDiscount, newMinOrder, newStartDate, newEndDate);

        if (request.getName() != null && !request.getName().isBlank()) {
            voucher.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            voucher.setDescription(request.getDescription().trim());
        }
        voucher.setDiscountType(newType);
        voucher.setDiscountValue(newValue);
        voucher.setMaxDiscountAmount(newMaxDiscount);
        voucher.setMinOrderAmount(newMinOrder);
        voucher.setStartDate(newStartDate);
        voucher.setEndDate(newEndDate);

        if (request.getUsageLimit() != null) {
            if (voucher.getUsedCount() != null && request.getUsageLimit() < voucher.getUsedCount()) {
                throw new BadRequestException(String.format("Giới hạn sử dụng mới (%d) không được nhỏ hơn số lượt đã dùng (%d).",
                        request.getUsageLimit(), voucher.getUsedCount()));
            }
            voucher.setUsageLimit(request.getUsageLimit());
        }

        if (request.getUserUsageLimit() != null) {
            voucher.setUserUsageLimit(request.getUserUsageLimit());
        }

        if (request.getIsActive() != null) {
            voucher.setActive(request.getIsActive());
        }

        Voucher savedVoucher = voucherRepository.save(voucher);
        return voucherMapper.toResponse(savedVoucher);
    }

    @Override
    public void deleteVoucher(UUID id) {
        if (id == null) {
            throw new BadRequestException("Mã định danh voucher không được để trống.");
        }

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));

        long usageCount = voucherUsageRepository.countByVoucherId(id);
        if (usageCount > 0) {
            // Đã có đơn hàng sử dụng mã này, thực hiện vô hiệu hóa thay vì xóa cứng để bảo toàn dữ liệu lịch sử
            voucher.setActive(false);
            voucherRepository.save(voucher);
            throw new BadRequestException("Voucher đã có " + usageCount + " lượt sử dụng trong các đơn hàng nên không thể xóa hoàn toàn. Hệ thống đã tự động vô hiệu hóa (tắt) voucher này.");
        }

        voucherRepository.delete(voucher);
    }

    @Override
    public VoucherResponse toggleVoucherStatus(UUID id) {
        if (id == null) {
            throw new BadRequestException("Mã định danh voucher không được để trống.");
        }

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));

        voucher.setActive(!voucher.isActive());
        Voucher savedVoucher = voucherRepository.save(voucher);
        return voucherMapper.toResponse(savedVoucher);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> getAllVouchers(AdminVoucherFilterRequest filter) {
        if (filter == null) {
            filter = new AdminVoucherFilterRequest();
        }

        Pageable pageable = createPageable(filter.getPage(), filter.getSize(), filter.getSortBy(), filter.getSortDir());

        Specification<Voucher> spec = VoucherSpecification.filter(
                filter.getEffectiveSearch(),
                filter.getDiscountType(),
                filter.getIsActive(),
                filter.getValidOnly()
        );

        Page<Voucher> page = voucherRepository.findAll(spec, pageable);
        return PageResponse.from(page, voucherMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherDetailResponse getVoucherDetail(UUID id) {
        if (id == null) {
            throw new BadRequestException("Mã định danh voucher không được để trống.");
        }

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));

        BigDecimal totalDiscount = voucherUsageRepository.sumDiscountAmountByVoucherId(id);
        long totalOrders = voucherUsageRepository.countByVoucherId(id);
        List<VoucherUsage> usages = voucherUsageRepository.findByVoucherIdOrderByUsedAtDesc(id);

        List<VoucherUsageSummaryResponse> usageResponses = usages.stream()
                .map(voucherMapper::toUsageSummaryResponse)
                .toList();

        return VoucherDetailResponse.builder()
                .voucher(voucherMapper.toResponse(voucher))
                .totalDiscountGiven(totalDiscount != null ? totalDiscount : BigDecimal.ZERO)
                .totalOrdersCount(totalOrders)
                .recentUsages(usageResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> getPublicVouchers(VoucherFilterRequest filter) {
        if (filter == null) {
            filter = new VoucherFilterRequest();
        }

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Voucher> spec = VoucherSpecification.filter(
                filter.getEffectiveSearch(),
                filter.getDiscountType(),
                true,
                true
        );

        Page<Voucher> page = voucherRepository.findAll(spec, pageable);
        return PageResponse.from(page, voucherMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAvailableVouchers() {
        return voucherRepository.findAvailableVouchers(Instant.now()).stream()
                .map(voucherMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Vui lòng cung cấp mã voucher.");
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với mã: " + code.trim().toUpperCase()));

        return voucherMapper.toResponse(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateVoucherResponse validateVoucher(String code, BigDecimal orderAmount, String userEmail) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Mã giảm giá không được để trống.");
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("Mã giảm giá '" + code.trim().toUpperCase() + "' không tồn tại."));

        User user = null;
        if (userEmail != null && !userEmail.isBlank()) {
            user = userRepository.findByEmail(userEmail.toLowerCase().trim()).orElse(null);
        }

        BigDecimal effectiveAmount = (orderAmount != null && orderAmount.compareTo(BigDecimal.ZERO) >= 0)
                ? orderAmount
                : BigDecimal.ZERO;

        validateVoucherEligibility(voucher, effectiveAmount, user);

        BigDecimal discountAmount = calculateDiscount(voucher, effectiveAmount);
        BigDecimal finalAmount = effectiveAmount.subtract(discountAmount).max(BigDecimal.ZERO);

        return ValidateVoucherResponse.builder()
                .valid(true)
                .voucherId(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .description(voucher.getDescription())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .discountAmount(discountAmount)
                .originalAmount(effectiveAmount)
                .finalAmount(finalAmount)
                .message("Áp dụng mã giảm giá thành công!")
                .build();
    }

    @Override
    public Voucher applyVoucherToOrder(String voucherCode, BigDecimal subtotalAmount, User user, Order order) {
        if (voucherCode == null || voucherCode.isBlank()) {
            order.setSubtotalAmount(subtotalAmount);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setTotalAmount(subtotalAmount);
            return null;
        }

        String normalizedCode = voucherCode.trim().toUpperCase();

        // Sử dụng khóa ghi PESSIMISTIC_WRITE để chống race condition khi kiểm tra lượt dùng
        Voucher voucher = voucherRepository.findByCodeWithLock(normalizedCode)
                .orElseThrow(() -> new BadRequestException("Mã giảm giá '" + normalizedCode + "' không tồn tại trong hệ thống."));

        validateVoucherEligibility(voucher, subtotalAmount, user);

        BigDecimal discountAmount = calculateDiscount(voucher, subtotalAmount);
        BigDecimal finalAmount = subtotalAmount.subtract(discountAmount).max(BigDecimal.ZERO);

        // Tăng số lượt đã sử dụng
        voucher.setUsedCount((voucher.getUsedCount() != null ? voucher.getUsedCount() : 0) + 1);
        voucherRepository.save(voucher);

        // Cập nhật thông tin chiết khấu vào đơn hàng
        order.setVoucher(voucher);
        order.setVoucherCode(voucher.getCode());
        order.setSubtotalAmount(subtotalAmount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(finalAmount);

        // Tạo bản ghi lịch sử sử dụng voucher
        VoucherUsage usage = VoucherUsage.builder()
                .voucher(voucher)
                .user(user)
                .order(order)
                .discountAmount(discountAmount)
                .build();
        voucherUsageRepository.save(usage);

        return voucher;
    }

    @Override
    public void restoreVoucherUsage(Order order) {
        if (order == null) {
            return;
        }

        Voucher voucher = order.getVoucher();
        if (voucher == null && order.getVoucherCode() != null) {
            voucher = voucherRepository.findByCodeIgnoreCase(order.getVoucherCode()).orElse(null);
        }

        if (voucher != null) {
            Voucher lockedVoucher = voucherRepository.findByIdWithLock(voucher.getId()).orElse(voucher);
            int currentUsed = lockedVoucher.getUsedCount() != null ? lockedVoucher.getUsedCount() : 0;
            if (currentUsed > 0) {
                lockedVoucher.setUsedCount(currentUsed - 1);
                voucherRepository.save(lockedVoucher);
            }
        }

        // Xóa bản ghi lịch sử sử dụng voucher gắn với đơn hàng này
        voucherUsageRepository.deleteByOrderId(order.getId());
    }

    private void validateVoucherEligibility(Voucher voucher, BigDecimal orderAmount, User user) {
        if (!voucher.isActive()) {
            throw new BadRequestException("Mã giảm giá '" + voucher.getCode() + "' hiện đang tạm khóa hoặc đã ngừng hoạt động.");
        }

        Instant now = Instant.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new BadRequestException("Chương trình giảm giá của mã '" + voucher.getCode() + "' chưa bắt đầu.");
        }

        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new BadRequestException("Mã giảm giá '" + voucher.getCode() + "' đã hết thời hạn sử dụng.");
        }

        if (voucher.getUsageLimit() != null && voucher.getUsedCount() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BadRequestException("Mã giảm giá '" + voucher.getCode() + "' đã hết lượt sử dụng trên toàn hệ thống.");
        }

        if (voucher.getMinOrderAmount() != null && orderAmount.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new BadRequestException(String.format(
                    "Đơn hàng phải có giá trị tối thiểu %s đ để có thể áp dụng mã '%s' (giá trị hiện tại: %s đ).",
                    voucher.getMinOrderAmount().setScale(0, RoundingMode.HALF_UP).toPlainString(),
                    voucher.getCode(),
                    orderAmount.setScale(0, RoundingMode.HALF_UP).toPlainString()
            ));
        }

        if (user != null) {
            long userUsedCount = voucherUsageRepository.countByVoucherIdAndUserId(voucher.getId(), user.getId());
            int limitPerUser = voucher.getUserUsageLimit() != null ? voucher.getUserUsageLimit() : 1;
            if (userUsedCount >= limitPerUser) {
                throw new BadRequestException(String.format("Bạn đã sử dụng hết lượt cho phép đối với mã giảm giá '%s' (tối đa %d lượt/khách hàng).",
                        voucher.getCode(), limitPerUser));
            }
        }
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount) {
        if (voucher == null || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                    discount = voucher.getMaxDiscountAmount();
                }
            }
        } else if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = voucher.getDiscountValue();
        }

        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateVoucherParameters(
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            Instant startDate,
            Instant endDate
    ) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new BadRequestException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu.");
        }

        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Giá trị giảm giá phải lớn hơn 0.");
        }

        if (discountType == DiscountType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BadRequestException("Giảm giá theo phần trăm không được vượt quá 100%.");
            }
        }

        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Mức giảm tối đa không được là số âm.");
        }

        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Giá trị đơn hàng tối thiểu không được là số âm.");
        }
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        String resolvedSortProperty = resolveSortProperty(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, resolvedSortProperty));
    }

    private String resolveSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        return switch (sortBy.trim()) {
            case "createdAt", "created_at" -> "createdAt";
            case "updatedAt", "updated_at" -> "updatedAt";
            case "startDate", "start_date" -> "startDate";
            case "endDate", "end_date" -> "endDate";
            case "usedCount", "used_count" -> "usedCount";
            case "discountValue", "discount_value" -> "discountValue";
            case "code" -> "code";
            default -> "createdAt";
        };
    }
}
