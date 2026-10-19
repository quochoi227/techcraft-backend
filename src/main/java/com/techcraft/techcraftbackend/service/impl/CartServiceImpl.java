package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.request.AddCartPcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.entity.CartItem;
import com.techcraft.techcraftbackend.entity.PcCase;
import com.techcraft.techcraftbackend.entity.PcCaseItem;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.CartMapper;
import com.techcraft.techcraftbackend.repository.CartItemRepository;
import com.techcraft.techcraftbackend.repository.PcCaseRepository;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PcCaseRepository pcCaseRepository;
    private final CartMapper cartMapper;

    @Override
    public CartItemResponse addCartItem(UUID userId, AddCartItemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        return addProductToUserCart(user, request);
    }

    @Override
    public CartItemResponse addCartItem(String email, AddCartItemRequest request) {
        User user = userRepository.findByEmail(email != null ? email.toLowerCase().trim() : null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));

        return addProductToUserCart(user, request);
    }

    @Override
    public CartItemResponse addPcCaseToCart(UUID userId, AddCartPcCaseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        return addPcCaseToUserCart(user, request);
    }

    @Override
    public CartItemResponse addPcCaseToCart(String email, AddCartPcCaseRequest request) {
        User user = userRepository.findByEmail(email != null ? email.toLowerCase().trim() : null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));

        return addPcCaseToUserCart(user, request);
    }

    private CartItemResponse addProductToUserCart(User user, AddCartItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Linh kiện này hiện đang ngừng kinh doanh.");
        }

        int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (availableStock <= 0) {
            throw new BadRequestException("Linh kiện đã hết hàng.");
        }

        int requestedQuantity = (request.getQuantity() != null && request.getQuantity() > 0)
                ? request.getQuantity()
                : 1;

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());

        CartItem cartItem;
        if (existingItemOpt.isPresent()) {
            cartItem = existingItemOpt.get();
            int currentQuantity = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;
            int newQuantity = currentQuantity + requestedQuantity;

            if (newQuantity > availableStock) {
                throw new BadRequestException(String.format(
                        "Số lượng linh kiện trong giỏ hàng (%d) vượt quá số lượng tồn kho hiện có (%d).",
                        newQuantity, availableStock
                ));
            }
            cartItem.setQuantity(newQuantity);
        } else {
            if (requestedQuantity > availableStock) {
                throw new BadRequestException(String.format(
                        "Số lượng yêu cầu (%d) vượt quá số lượng tồn kho hiện có (%d).",
                        requestedQuantity, availableStock
                ));
            }
            cartItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(requestedQuantity)
                    .build();
        }

        CartItem savedItem = cartItemRepository.save(cartItem);
        return cartMapper.toResponse(savedItem);
    }

    private CartItemResponse addPcCaseToUserCart(User user, AddCartPcCaseRequest request) {
        PcCase pcCase = pcCaseRepository.findById(request.getPcCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình PC với ID: " + request.getPcCaseId()));

        if (!pcCase.isActive()) {
            throw new BadRequestException("Cấu hình PC này hiện đang ngừng kinh doanh.");
        }

        List<PcCaseItem> items = pcCase.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Cấu hình PC chưa có danh sách linh kiện hợp lệ.");
        }

        int availablePcCaseStock = Integer.MAX_VALUE;
        for (PcCaseItem item : items) {
            Product product = item.getProduct();
            if (product == null) {
                throw new BadRequestException(String.format(
                        "Linh kiện thuộc danh mục %s không tồn tại trong hệ thống.",
                        item.getCategory()
                ));
            }
            if (!product.isActive()) {
                throw new BadRequestException(String.format(
                        "Linh kiện '%s' (%s) trong cấu hình PC hiện đang ngừng kinh doanh.",
                        product.getName(), item.getCategory()
                ));
            }
            int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (stock <= 0) {
                throw new BadRequestException(String.format(
                        "Linh kiện '%s' (%s) trong cấu hình PC đã hết hàng.",
                        product.getName(), item.getCategory()
                ));
            }
            if (stock < availablePcCaseStock) {
                availablePcCaseStock = stock;
            }
        }

        if (availablePcCaseStock <= 0 || availablePcCaseStock == Integer.MAX_VALUE) {
            throw new BadRequestException("Cấu hình PC này hiện không đủ linh kiện trong kho để lắp ráp.");
        }

        int requestedQuantity = (request.getQuantity() != null && request.getQuantity() > 0)
                ? request.getQuantity()
                : 1;

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserIdAndPcCaseId(user.getId(), pcCase.getId());

        CartItem cartItem;
        if (existingItemOpt.isPresent()) {
            cartItem = existingItemOpt.get();
            int currentQuantity = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;
            int newQuantity = currentQuantity + requestedQuantity;

            if (newQuantity > availablePcCaseStock) {
                throw new BadRequestException(String.format(
                        "Số lượng cấu hình PC trong giỏ hàng (%d) vượt quá số lượng tối đa có thể lắp ráp hiện tại (%d).",
                        newQuantity, availablePcCaseStock
                ));
            }
            cartItem.setQuantity(newQuantity);
        } else {
            if (requestedQuantity > availablePcCaseStock) {
                throw new BadRequestException(String.format(
                        "Số lượng yêu cầu (%d) vượt quá số lượng tối đa có thể lắp ráp hiện tại (%d).",
                        requestedQuantity, availablePcCaseStock
                ));
            }
            cartItem = CartItem.builder()
                    .user(user)
                    .pcCase(pcCase)
                    .quantity(requestedQuantity)
                    .build();
        }

        CartItem savedItem = cartItemRepository.save(cartItem);
        return cartMapper.toResponse(savedItem);
    }
}
