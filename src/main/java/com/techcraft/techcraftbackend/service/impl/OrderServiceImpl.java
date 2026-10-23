package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.request.CreateOrderItemRequest;
import com.techcraft.techcraftbackend.dto.request.CreateOrderRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateOrderStatusRequest;
import com.techcraft.techcraftbackend.dto.response.OrderResponse;
import com.techcraft.techcraftbackend.entity.*;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.PaymentMethod;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.AppException;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.OrderMapper;
import com.techcraft.techcraftbackend.repository.*;
import com.techcraft.techcraftbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PcCaseRepository pcCaseRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse createOrder(String email, CreateOrderRequest request) {
        User user = userRepository.findByEmail(email != null ? email.toLowerCase().trim() : null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));

        return processCreateOrder(user, request);
    }

    @Override
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        return processCreateOrder(user, request);
    }

    @Override
    public OrderResponse cancelOrder(String email, UUID orderId) {
        User user = userRepository.findByEmail(email != null ? email.toLowerCase().trim() : null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));

        return processCancelOrder(user, orderId);
    }

    @Override
    public OrderResponse cancelOrder(UUID userId, UUID orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        return processCancelOrder(user, orderId);
    }

    @Override
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        if (orderId == null) {
            throw new BadRequestException("Mã đơn hàng không được để trống.");
        }

        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("Trạng thái đơn hàng không được để trống.");
        }

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        if (currentStatus == newStatus) {
            throw new BadRequestException(String.format("Đơn hàng hiện tại đã ở trạng thái %s.", currentStatus));
        }

        validateStatusTransition(currentStatus, newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            restoreOrderStock(order);
        }

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("Không thể thay đổi trạng thái của đơn hàng đã bị hủy.");
        }

        if (currentStatus == OrderStatus.DELIVERED) {
            throw new BadRequestException("Không thể thay đổi trạng thái của đơn hàng đã giao thành công.");
        }

        switch (currentStatus) {
            case PENDING:
                if (newStatus != OrderStatus.CONFIRMED && newStatus != OrderStatus.CANCELLED) {
                    throw new BadRequestException("Đơn hàng đang chờ xử lý chỉ có thể chuyển sang CONFIRMED hoặc CANCELLED.");
                }
                break;
            case CONFIRMED:
                if (newStatus != OrderStatus.SHIPPING && newStatus != OrderStatus.CANCELLED) {
                    throw new BadRequestException("Đơn hàng đã xác nhận chỉ có thể chuyển sang SHIPPING hoặc CANCELLED.");
                }
                break;
            case SHIPPING:
                if (newStatus != OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
                    throw new BadRequestException("Đơn hàng đang giao chỉ có thể chuyển sang DELIVERED hoặc CANCELLED.");
                }
                break;
            default:
                throw new BadRequestException("Trạng thái chuyển đổi không hợp lệ.");
        }
    }

    @Override
    public void restoreOrderStock(Order order) {
        if (order == null || order.getItems() == null) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            int quantity = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 1;

            if (item.getProduct() != null) {
                Product product = item.getProduct();
                int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
                product.setStockQuantity(currentStock + quantity);
                productRepository.save(product);
            } else if (item.getPcCase() != null) {
                PcCase pcCase = item.getPcCase();
                if (pcCase.getItems() != null) {
                    for (PcCaseItem caseItem : pcCase.getItems()) {
                        Product comp = caseItem.getProduct();
                        if (comp != null) {
                            int currentStock = comp.getStockQuantity() != null ? comp.getStockQuantity() : 0;
                            comp.setStockQuantity(currentStock + quantity);
                            productRepository.save(comp);
                        }
                    }
                }
            }
        }
    }

    private OrderResponse processCancelOrder(User user, UUID orderId) {
        if (orderId == null) {
            throw new BadRequestException("Mã đơn hàng không được để trống.");
        }

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        boolean isOwner = order.getUser() != null && order.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AppException("Bạn không có quyền hủy đơn hàng này.", HttpStatus.FORBIDDEN);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng đã được hủy trước đó.");
        }

        if (order.getStatus() == OrderStatus.SHIPPING) {
            throw new BadRequestException("Không thể hủy đơn hàng đang trong quá trình vận chuyển.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Không thể hủy đơn hàng đã giao thành công.");
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new BadRequestException("Không thể hủy đơn hàng khi đã được xác nhận.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng khi đơn hàng đang ở trạng thái chờ xử lý.");
        }

        // Hoàn lại số lượng tồn kho của các sản phẩm và linh kiện PC Case
        restoreOrderStock(order);

        // Chuyển trạng thái đơn hàng sang CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    private OrderResponse processCreateOrder(User user, CreateOrderRequest request) {
        // 1. Xác định địa chỉ nhận hàng
        String shippingAddress = (request != null && request.getShippingAddress() != null)
                ? request.getShippingAddress().trim()
                : null;

        if (shippingAddress == null || shippingAddress.isEmpty()) {
            shippingAddress = (user.getAddress() != null) ? user.getAddress().trim() : null;
        }

        if (shippingAddress == null || shippingAddress.isEmpty()) {
            throw new BadRequestException("Vui lòng cung cấp địa chỉ nhận hàng.");
        }

        // 2. Xác định danh sách món hàng cần đặt
        List<CreateOrderItemRequest> rawItems = (request != null && request.getItems() != null)
                ? request.getItems()
                : Collections.emptyList();

        boolean isFromCart = false;
        if (rawItems.isEmpty()) {
            isFromCart = true;
            List<CartItem> cartItems = cartItemRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(user.getId());
            if (cartItems == null || cartItems.isEmpty()) {
                throw new BadRequestException("Giỏ hàng của bạn đang trống. Vui lòng chọn sản phẩm để đặt hàng.");
            }

            rawItems = cartItems.stream()
                    .map(c -> {
                        CreateOrderItemRequest itemReq = new CreateOrderItemRequest();
                        if (c.getProduct() != null) {
                            itemReq.setProductId(c.getProduct().getId());
                        } else if (c.getPcCase() != null) {
                            itemReq.setPcCaseId(c.getPcCase().getId());
                        }
                        itemReq.setQuantity(c.getQuantity());
                        return itemReq;
                    })
                    .toList();
        }

        // 3. Chuẩn hóa và tổng hợp số lượng (gộp duplicate nếu có)
        Map<UUID, Integer> productQuantities = new LinkedHashMap<>();
        Map<UUID, Integer> pcCaseQuantities = new LinkedHashMap<>();

        for (CreateOrderItemRequest item : rawItems) {
            UUID productId = item.getProductId();
            UUID pcCaseId = item.getPcCaseId();

            if (productId == null && pcCaseId == null) {
                throw new BadRequestException("Mỗi mục đơn hàng phải chứa mã linh kiện hoặc mã cấu hình PC.");
            }
            if (productId != null && pcCaseId != null) {
                throw new BadRequestException("Mỗi mục đơn hàng chỉ được là linh kiện lẻ hoặc cấu hình PC, không được chứa cả hai.");
            }

            int qty = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 1;
            if (productId != null) {
                productQuantities.merge(productId, qty, Integer::sum);
            } else {
                pcCaseQuantities.merge(pcCaseId, qty, Integer::sum);
            }
        }

        // 4. Lấy thực thể và kiểm tra tính hợp lệ
        Map<UUID, Product> productsMap = new HashMap<>();
        Map<UUID, PcCase> pcCasesMap = new HashMap<>();

        for (UUID productId : productQuantities.keySet()) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + productId));

            if (!product.isActive()) {
                throw new BadRequestException("Linh kiện '" + product.getName() + "' hiện đang ngừng kinh doanh.");
            }
            productsMap.put(productId, product);
        }

        for (UUID pcCaseId : pcCaseQuantities.keySet()) {
            PcCase pcCase = pcCaseRepository.findById(pcCaseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình PC với ID: " + pcCaseId));

            if (!pcCase.isActive()) {
                throw new BadRequestException("Cấu hình PC '" + pcCase.getName() + "' hiện đang ngừng kinh doanh.");
            }

            List<PcCaseItem> caseItems = pcCase.getItems();
            if (caseItems == null || caseItems.isEmpty()) {
                throw new BadRequestException("Cấu hình PC '" + pcCase.getName() + "' chưa có danh sách linh kiện hợp lệ.");
            }

            for (PcCaseItem caseItem : caseItems) {
                Product comp = caseItem.getProduct();
                if (comp == null) {
                    throw new BadRequestException("Linh kiện trong cấu hình PC '" + pcCase.getName() + "' không tồn tại trong hệ thống.");
                }
                if (!comp.isActive()) {
                    throw new BadRequestException(String.format("Linh kiện '%s' (%s) trong cấu hình PC '%s' hiện đang ngừng kinh doanh.",
                            comp.getName(), caseItem.getCategory(), pcCase.getName()));
                }
                productsMap.putIfAbsent(comp.getId(), comp);
            }

            pcCasesMap.put(pcCaseId, pcCase);
        }

        // 5. Tính toán tổng nhu cầu tồn kho trên từng Product
        Map<UUID, Integer> totalProductRequired = new HashMap<>();
        productQuantities.forEach((pId, qty) -> totalProductRequired.merge(pId, qty, Integer::sum));

        pcCaseQuantities.forEach((cId, cQty) -> {
            PcCase pcCase = pcCasesMap.get(cId);
            for (PcCaseItem caseItem : pcCase.getItems()) {
                UUID compId = caseItem.getProduct().getId();
                totalProductRequired.merge(compId, cQty, Integer::sum);
            }
        });

        // 6. Kiểm tra số lượng tồn kho khả dụng
        for (Map.Entry<UUID, Integer> entry : totalProductRequired.entrySet()) {
            UUID productId = entry.getKey();
            int requiredQty = entry.getValue();
            Product product = productsMap.get(productId);

            int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (availableStock < requiredQty) {
                throw new BadRequestException(String.format(
                        "Linh kiện '%s' không đủ số lượng tồn kho để đáp ứng đơn hàng (cần %d, hiện có %d).",
                        product.getName(), requiredQty, availableStock
                ));
            }
        }

        // 7. Khấu trừ tồn kho
        for (Map.Entry<UUID, Integer> entry : totalProductRequired.entrySet()) {
            UUID productId = entry.getKey();
            int requiredQty = entry.getValue();
            Product product = productsMap.get(productId);

            product.setStockQuantity(product.getStockQuantity() - requiredQty);
            productRepository.save(product);
        }

        // 8. Khởi tạo Order và OrderItems
        PaymentMethod paymentMethod = (request != null && request.getPaymentMethod() != null)
                ? request.getPaymentMethod()
                : PaymentMethod.COD;

        String note = (request != null) ? request.getNote() : null;

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentMethod(paymentMethod)
                .shippingAddress(shippingAddress)
                .note(note)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<UUID, Integer> entry : productQuantities.entrySet()) {
            Product product = productsMap.get(entry.getKey());
            int quantity = entry.getValue();
            BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        for (Map.Entry<UUID, Integer> entry : pcCaseQuantities.entrySet()) {
            PcCase pcCase = pcCasesMap.get(entry.getKey());
            int quantity = entry.getValue();
            BigDecimal unitPrice = pcCase.getTotalPrice() != null ? pcCase.getTotalPrice() : BigDecimal.ZERO;

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .pcCase(pcCase)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // 9. Dọn dẹp giỏ hàng
        if (isFromCart) {
            cartItemRepository.deleteByUserId(user.getId());
        } else {
            productQuantities.keySet().forEach(pId -> cartItemRepository.deleteByUserIdAndProductId(user.getId(), pId));
            pcCaseQuantities.keySet().forEach(cId -> cartItemRepository.deleteByUserIdAndPcCaseId(user.getId(), cId));
        }

        return orderMapper.toResponse(savedOrder);
    }
}
