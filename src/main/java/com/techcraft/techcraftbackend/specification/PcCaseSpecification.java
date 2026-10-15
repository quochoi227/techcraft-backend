package com.techcraft.techcraftbackend.specification;

import com.techcraft.techcraftbackend.entity.PcCase;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class PcCaseSpecification {

    private PcCaseSpecification() {
    }

    public static Specification<PcCase> filter(
            String keyword,
            String useCase,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isActive
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descMatch = cb.and(
                        cb.isNotNull(root.get("description")),
                        cb.like(cb.lower(root.get("description")), pattern)
                );
                Predicate useCaseMatch = cb.and(
                        cb.isNotNull(root.get("useCase")),
                        cb.like(cb.lower(root.get("useCase")), pattern)
                );
                predicates.add(cb.or(nameMatch, descMatch, useCaseMatch));
            }

            if (StringUtils.hasText(useCase)) {
                predicates.add(cb.equal(cb.lower(root.get("useCase")), useCase.trim().toLowerCase()));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalPrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalPrice"), maxPrice));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
