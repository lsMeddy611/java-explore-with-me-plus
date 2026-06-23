package ru.practicum.dto.user.param_objects;

import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.Objects;

public record AdminUserFilter(
        @Min(0L)
        Integer from,
        @Min(1)
        Integer size,
        List<Long> ids
) {
    public AdminUserFilter {
        from = Objects.requireNonNullElse(from, 0);
        size = Objects.requireNonNullElse(size, 10);
        ids = Objects.requireNonNullElse(ids, List.of());
    }
}