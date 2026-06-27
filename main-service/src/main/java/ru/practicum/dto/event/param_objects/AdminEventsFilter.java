package ru.practicum.dto.event.param_objects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public record AdminEventsFilter(
        List<Long> users,
        List<String> states,
        List<Long> categories,
        String rangeStart,
        String rangeEnd,
        Integer from,
        Integer size
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AdminEventsFilter {
        from = Objects.requireNonNullElse(from, 0);
        size = Objects.requireNonNullElse(size, 10);
        users = Objects.requireNonNullElse(users, List.of());
        states = Objects.requireNonNullElse(states, List.of());
        categories = Objects.requireNonNullElse(categories, List.of());
    }

    public LocalDateTime getRangeStartDateTime() {
        if (rangeStart == null || rangeStart.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(rangeStart, FORMATTER);
    }

    public LocalDateTime getRangeEndDateTime() {
        if (rangeEnd == null || rangeEnd.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(rangeEnd, FORMATTER);
    }
}
