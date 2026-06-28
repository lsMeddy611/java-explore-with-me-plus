package ru.practicum.dto.user.param_objects;

import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.Objects;

public record AdminUserFilter(
        @Min(value = 0L, message = "параметр from должен быть равен или больше 0")
        Integer from,
        @Min(value = 1, message =  "параметр size должен быть положительным")
        Integer size,
        List<Long> ids
) {
    public AdminUserFilter {
        from = Objects.requireNonNullElse(from, 0);
        size = Objects.requireNonNullElse(size, 10);
        ids = Objects.requireNonNullElse(ids, List.of());
    }
}