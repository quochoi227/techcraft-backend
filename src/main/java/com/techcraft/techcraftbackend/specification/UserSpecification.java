package com.techcraft.techcraftbackend.specification;

import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> filter(
            String keyword,
            Role role,
            Boolean active
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (StringUtils.hasText(keyword)) {
                String trimmed = keyword.trim();
                String pattern = "%" + trimmed.toLowerCase() + "%";

                List<Predicate> keywordPredicates = new ArrayList<>();

                keywordPredicates.add(cb.and(cb.isNotNull(root.get("email")), cb.like(cb.lower(root.get("email")), pattern)));
                keywordPredicates.add(cb.and(cb.isNotNull(root.get("fullName")), cb.like(cb.lower(root.get("fullName")), pattern)));
                keywordPredicates.add(cb.and(cb.isNotNull(root.get("phone")), cb.like(cb.lower(root.get("phone")), pattern)));

                try {
                    UUID userId = UUID.fromString(trimmed);
                    keywordPredicates.add(cb.equal(root.get("id"), userId));
                } catch (IllegalArgumentException ignored) {
                    // Không phải UUID hợp lệ, bỏ qua
                }

                predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
