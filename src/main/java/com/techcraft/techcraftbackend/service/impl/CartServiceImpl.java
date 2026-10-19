package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.entity.CartItem;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.CartMapper;
import com.techcraft.techcraftbackend.repository.CartItemRepository;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
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
}
