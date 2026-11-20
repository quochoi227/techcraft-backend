package com.techcraft.techcraftbackend.specification;

import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.PaymentMethod;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OrderSpecification {

    private OrderSpecification() {
    }

    public static Specification<Order> filter(
            OrderStatus status,
            String keyword,
            PaymentMethod paymentMethod
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (paymentMethod != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            }

            if (StringUtils.hasText(keyword)) {
                String trimmed = keyword.trim();
                String pattern = "%" + trimmed.toLowerCase() + "%";

                Join<Order, User> userJoin = root.join("user", JoinType.LEFT);

                List<Predicate> keywordPredicates = new ArrayList<>();

                // Tìm theo các thông tin người dùng và địa chỉ nhận hàng
                keywordPredicates.add(cb.and(cb.isNotNull(userJoin.get("email")), cb.like(cb.lower(userJoin.get("email")), pattern)));
                keywordPredicates.add(cb.and(cb.isNotNull(userJoin.get("fullName")), cb.like(cb.lower(userJoin.get("fullName")), pattern)));
                keywordPredicates.add(cb.and(cb.isNotNull(userJoin.get("phone")), cb.like(cb.lower(userJoin.get("phone")), pattern)));
                keywordPredicates.add(cb.and(cb.isNotNull(root.get("shippingAddress")), cb.like(cb.lower(root.get("shippingAddress")), pattern)));

                // Nếu keyword là UUID hợp lệ, so khớp thêm mã đơn hàng
                try {
                    UUID orderId = UUID.fromString(trimmed);
                    keywordPredicates.add(cb.equal(root.get("id"), orderId));
                } catch (IllegalArgumentException ignored) {
                    // Không phải định dạng UUID, bỏ qua so khớp theo ID
                }

                predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
