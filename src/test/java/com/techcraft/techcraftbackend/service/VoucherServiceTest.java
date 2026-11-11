package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateVoucherRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateVoucherRequest;
import com.techcraft.techcraftbackend.dto.response.ValidateVoucherResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherResponse;
import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.entity.Voucher;
import com.techcraft.techcraftbackend.entity.VoucherUsage;
import com.techcraft.techcraftbackend.enums.DiscountType;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.VoucherMapper;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.repository.VoucherRepository;
import com.techcraft.techcraftbackend.repository.VoucherUsageRepository;
import com.techcraft.techcraftbackend.service.impl.VoucherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherUsageRepository voucherUsageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VoucherMapper voucherMapper;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private User sampleUser;
    private Voucher samplePercentVoucher;
    private Voucher sampleFixedVoucher;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("testuser@techcraft.com")
                .fullName("Test Customer")
                .role(Role.USER)
                .active(true)
                .build();

        samplePercentVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .code("SUMMER10")
                .name("Giảm 10% tối đa 100K")
                .description("Voucher mùa hè cho đơn từ 500k")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .maxDiscountAmount(BigDecimal.valueOf(100000))
                .minOrderAmount(BigDecimal.valueOf(500000))
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .usageLimit(100)
                .usedCount(5)
                .userUsageLimit(1)
                .active(true)
                .build();

        sampleFixedVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .code("GIAM50K")
                .name("Giảm trực tiếp 50K")
                .description("Áp dụng đơn từ 200k")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .usageLimit(50)
                .usedCount(0)
                .userUsageLimit(2)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Tạo voucher thành công khi dữ liệu hợp lệ")
    void createVoucher_ValidRequest_Success() {
        CreateVoucherRequest request = CreateVoucherRequest.builder()
                .code("WELCOME2026")
                .name("Chào đón khách hàng mới")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(15))
                .maxDiscountAmount(BigDecimal.valueOf(200000))
                .minOrderAmount(BigDecimal.valueOf(1000000))
                .startDate(Instant.now())
                .endDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .usageLimit(100)
                .userUsageLimit(1)
                .isActive(true)
                .build();

        Voucher mappedEntity = Voucher.builder()
                .code("WELCOME2026")
                .name(request.getName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        VoucherResponse expectedResponse = VoucherResponse.builder()
                .code("WELCOME2026")
                .name(request.getName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .valid(true)
                .build();

        when(voucherRepository.existsByCodeIgnoreCase("WELCOME2026")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(mappedEntity);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(voucherMapper.toResponse(any(Voucher.class))).thenReturn(expectedResponse);

        VoucherResponse response = voucherService.createVoucher(request);

        assertNotNull(response);
        assertEquals("WELCOME2026", response.getCode());
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Tạo voucher thất bại khi mã voucher đã tồn tại")
    void createVoucher_DuplicateCode_ThrowsBadRequestException() {
        CreateVoucherRequest request = CreateVoucherRequest.builder()
                .code("SUMMER10")
                .name("Giảm hè")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .startDate(Instant.now())
                .endDate(Instant.now().plus(10, ChronoUnit.DAYS))
                .build();

        when(voucherRepository.existsByCodeIgnoreCase("SUMMER10")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> voucherService.createVoucher(request));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo voucher thất bại khi ngày kết thúc trước ngày bắt đầu")
    void createVoucher_EndDateBeforeStartDate_ThrowsBadRequestException() {
        CreateVoucherRequest request = CreateVoucherRequest.builder()
                .code("INVALIDDATE")
                .name("Sai ngày")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .startDate(Instant.now().plus(10, ChronoUnit.DAYS))
                .endDate(Instant.now())
                .build();

        when(voucherRepository.existsByCodeIgnoreCase("INVALIDDATE")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> voucherService.createVoucher(request));
    }

    @Test
    @DisplayName("Tạo voucher thất bại khi phần trăm giảm giá vượt quá 100%")
    void createVoucher_PercentageOver100_ThrowsBadRequestException() {
        CreateVoucherRequest request = CreateVoucherRequest.builder()
                .code("OVER100")
                .name("Giảm quá trần")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(150))
                .startDate(Instant.now())
                .endDate(Instant.now().plus(10, ChronoUnit.DAYS))
                .build();

        when(voucherRepository.existsByCodeIgnoreCase("OVER100")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> voucherService.createVoucher(request));
    }

    @Test
    @DisplayName("Cập nhật voucher thành công")
    void updateVoucher_Success() {
        UUID id = samplePercentVoucher.getId();
        UpdateVoucherRequest request = UpdateVoucherRequest.builder()
                .name("Tên mới cập nhật")
                .usageLimit(200)
                .build();

        when(voucherRepository.findById(id)).thenReturn(Optional.of(samplePercentVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(voucherMapper.toResponse(any(Voucher.class))).thenReturn(VoucherResponse.builder().name("Tên mới cập nhật").build());

        VoucherResponse response = voucherService.updateVoucher(id, request);

        assertNotNull(response);
        assertEquals("Tên mới cập nhật", response.getName());
        assertEquals(200, samplePercentVoucher.getUsageLimit());
    }

    @Test
    @DisplayName("Cập nhật voucher thất bại khi giới hạn lượt dùng mới nhỏ hơn số lượt đã dùng")
    void updateVoucher_LimitLessThanUsedCount_ThrowsBadRequestException() {
        UUID id = samplePercentVoucher.getId(); // usedCount = 5
        UpdateVoucherRequest request = UpdateVoucherRequest.builder()
                .usageLimit(3) // Nhỏ hơn 5
                .build();

        when(voucherRepository.findById(id)).thenReturn(Optional.of(samplePercentVoucher));

        assertThrows(BadRequestException.class, () -> voucherService.updateVoucher(id, request));
    }

    @Test
    @DisplayName("Xóa voucher thành công khi chưa có đơn hàng nào sử dụng")
    void deleteVoucher_NoUsages_HardDelete() {
        UUID id = sampleFixedVoucher.getId();
        when(voucherRepository.findById(id)).thenReturn(Optional.of(sampleFixedVoucher));
        when(voucherUsageRepository.countByVoucherId(id)).thenReturn(0L);

        voucherService.deleteVoucher(id);

        verify(voucherRepository).delete(sampleFixedVoucher);
    }

    @Test
    @DisplayName("Xóa voucher đã có đơn hàng sử dụng sẽ chuyển sang vô hiệu hóa và thông báo")
    void deleteVoucher_WithUsages_DeactivatesAndThrowsException() {
        UUID id = samplePercentVoucher.getId();
        when(voucherRepository.findById(id)).thenReturn(Optional.of(samplePercentVoucher));
        when(voucherUsageRepository.countByVoucherId(id)).thenReturn(3L);

        assertThrows(BadRequestException.class, () -> voucherService.deleteVoucher(id));
        assertFalse(samplePercentVoucher.isActive());
        verify(voucherRepository).save(samplePercentVoucher);
        verify(voucherRepository, never()).delete(any(Voucher.class));
    }

    @Test
    @DisplayName("Validate voucher phần trăm thành công và tính đúng số tiền giảm")
    void validateVoucher_Percentage_Success() {
        when(voucherRepository.findByCodeIgnoreCase("SUMMER10")).thenReturn(Optional.of(samplePercentVoucher));
        when(userRepository.findByEmail("testuser@techcraft.com")).thenReturn(Optional.of(sampleUser));
        when(voucherUsageRepository.countByVoucherIdAndUserId(samplePercentVoucher.getId(), sampleUser.getId())).thenReturn(0L);

        // Đơn 800.000đ -> Giảm 10% = 80.000đ (chưa chạm trần 100.000đ)
        ValidateVoucherResponse response = voucherService.validateVoucher(
                "SUMMER10",
                BigDecimal.valueOf(800000),
                "testuser@techcraft.com"
        );

        assertTrue(response.isValid());
        assertEquals(0, BigDecimal.valueOf(80000).compareTo(response.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(720000).compareTo(response.getFinalAmount()));
    }

    @Test
    @DisplayName("Validate voucher phần trăm chạm mức giảm tối đa (maxDiscountAmount)")
    void validateVoucher_Percentage_CappedAtMaxDiscount() {
        when(voucherRepository.findByCodeIgnoreCase("SUMMER10")).thenReturn(Optional.of(samplePercentVoucher));
        when(userRepository.findByEmail("testuser@techcraft.com")).thenReturn(Optional.of(sampleUser));
        when(voucherUsageRepository.countByVoucherIdAndUserId(samplePercentVoucher.getId(), sampleUser.getId())).thenReturn(0L);

        // Đơn 2.000.000đ -> 10% là 200.000đ nhưng trần maxDiscountAmount = 100.000đ
        ValidateVoucherResponse response = voucherService.validateVoucher(
                "SUMMER10",
                BigDecimal.valueOf(2000000),
                "testuser@techcraft.com"
        );

        assertTrue(response.isValid());
        assertEquals(0, BigDecimal.valueOf(100000).compareTo(response.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(1900000).compareTo(response.getFinalAmount()));
    }

    @Test
    @DisplayName("Validate voucher số tiền cố định thành công")
    void validateVoucher_FixedAmount_Success() {
        when(voucherRepository.findByCodeIgnoreCase("GIAM50K")).thenReturn(Optional.of(sampleFixedVoucher));

        // Đơn 300.000đ -> Giảm 50.000đ còn 250.000đ
        ValidateVoucherResponse response = voucherService.validateVoucher(
                "GIAM50K",
                BigDecimal.valueOf(300000),
                null
        );

        assertTrue(response.isValid());
        assertEquals(0, BigDecimal.valueOf(50000).compareTo(response.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(250000).compareTo(response.getFinalAmount()));
    }

    @Test
    @DisplayName("Validate voucher thất bại khi đơn hàng chưa đạt giá trị tối thiểu (minOrderAmount)")
    void validateVoucher_BelowMinOrderAmount_ThrowsBadRequestException() {
        when(voucherRepository.findByCodeIgnoreCase("SUMMER10")).thenReturn(Optional.of(samplePercentVoucher)); // minOrder = 500k

        // Đơn 300.000đ < 500.000đ
        assertThrows(BadRequestException.class, () -> voucherService.validateVoucher(
                "SUMMER10",
                BigDecimal.valueOf(300000),
                null
        ));
    }

    @Test
    @DisplayName("Validate voucher thất bại khi voucher đang bị khóa")
    void validateVoucher_Inactive_ThrowsBadRequestException() {
        samplePercentVoucher.setActive(false);
        when(voucherRepository.findByCodeIgnoreCase("SUMMER10")).thenReturn(Optional.of(samplePercentVoucher));

        assertThrows(BadRequestException.class, () -> voucherService.validateVoucher(
                "SUMMER10",
                BigDecimal.valueOf(1000000),
                null
        ));
    }

    @Test
    @DisplayName("Validate voucher thất bại khi voucher đã hết hạn")
    void validateVoucher_Expired_ThrowsBadRequestException() {
        samplePercentVoucher.setEndDate(Instant.now().minus(2, ChronoUnit.DAYS));
        when(voucherRepository.findByCodeIgnoreCase("SUMMER10")).thenReturn(Optional.of(samplePercentVoucher));

        assertThrows(BadRequestException.class, () -> voucherService.validateVoucher(
                "SUMMER10",
                BigDecimal.valueOf(1000000),
                null
        ));
    }

    @Test
    @DisplayName("Validate voucher thất bại khi voucher đã hết lượt sử dụng toàn hệ thống")
    void validateVoucher_GlobalLimitReached_ThrowsBadRequestException() {
        samplePercentVoucher.setUsageLimit(10);
        samplePercentVoucher.setUsedCount(10);
        when(voucherRepository.findByCodeIgnoreCase("SUMMER10")).thenReturn(Optional.of(samplePercentVoucher));

        assertThrows(BadRequestException.class, () -> voucherService.validateVoucher(
                "SUMMER10",
                BigDecimal.valueOf(1000000),
                null
        ));
    }

    @Test
    @DisplayName("Validate voucher thất bại khi user đã dùng hết lượt cho phép")
    void validateVoucher_UserLimitReached_ThrowsBadRequestException() {
        when(voucherRepository.findByCodeIgnoreCase("SUMMER10")).thenReturn(Optional.of(samplePercentVoucher));
        when(userRepository.findByEmail("testuser@techcraft.com")).thenReturn(Optional.of(sampleUser));
        when(voucherUsageRepository.countByVoucherIdAndUserId(samplePercentVoucher.getId(), sampleUser.getId())).thenReturn(1L); // limit = 1

        assertThrows(BadRequestException.class, () -> voucherService.validateVoucher(
                "SUMMER10",
                BigDecimal.valueOf(1000000),
                "testuser@techcraft.com"
        ));
    }

    @Test
    @DisplayName("Áp dụng voucher vào đơn hàng thành công")
    void applyVoucherToOrder_Success() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .build();

        BigDecimal subtotal = BigDecimal.valueOf(1000000);

        when(voucherRepository.findByCodeWithLock("SUMMER10")).thenReturn(Optional.of(samplePercentVoucher));
        when(voucherUsageRepository.countByVoucherIdAndUserId(samplePercentVoucher.getId(), sampleUser.getId())).thenReturn(0L);

        int initialUsedCount = samplePercentVoucher.getUsedCount();
        Voucher result = voucherService.applyVoucherToOrder("SUMMER10", subtotal, sampleUser, order);

        assertNotNull(result);
        assertEquals(initialUsedCount + 1, samplePercentVoucher.getUsedCount());
        assertEquals("SUMMER10", order.getVoucherCode());
        assertEquals(0, BigDecimal.valueOf(1000000).compareTo(order.getSubtotalAmount()));
        assertEquals(0, BigDecimal.valueOf(100000).compareTo(order.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(900000).compareTo(order.getTotalAmount()));

        verify(voucherRepository).save(samplePercentVoucher);
        verify(voucherUsageRepository).save(any(VoucherUsage.class));
    }

    @Test
    @DisplayName("Hoàn trả lượt dùng voucher khi đơn hàng bị hủy")
    void restoreVoucherUsage_Success() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .voucher(samplePercentVoucher)
                .voucherCode(samplePercentVoucher.getCode())
                .build();

        samplePercentVoucher.setUsedCount(5);

        when(voucherRepository.findByIdWithLock(samplePercentVoucher.getId())).thenReturn(Optional.of(samplePercentVoucher));

        voucherService.restoreVoucherUsage(order);

        assertEquals(4, samplePercentVoucher.getUsedCount());
        verify(voucherRepository).save(samplePercentVoucher);
        verify(voucherUsageRepository).deleteByOrderId(order.getId());
    }
}
