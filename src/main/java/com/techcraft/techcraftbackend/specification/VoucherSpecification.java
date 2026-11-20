package com.techcraft.techcraftbackend.specification;

import com.techcraft.techcraftbackend.entity.Voucher;
import com.techcraft.techcraftbackend.enums.DiscountType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class VoucherSpecification {

    private VoucherSpecification() {
    }

    public static Specification<Voucher> filter(
            String keyword,
            DiscountType discountType,
            Boolean isActive,
            Boolean validOnly
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            if (discountType != null) {
                predicates.add(cb.equal(root.get("discountType"), discountType));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("active"), isActive));
            }

            if (Boolean.TRUE.equals(validOnly)) {
                Instant now = Instant.now();
                predicates.add(cb.isTrue(root.get("active")));
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), now));
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), now));
                predicates.add(cb.or(
                        cb.isNull(root.get("usageLimit")),
                        cb.lt(root.get("usedCount"), root.get("usageLimit"))
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
