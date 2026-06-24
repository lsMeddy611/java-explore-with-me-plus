package ru.practicum.dto.event.param_objects;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public record PublicEventsFilter(
        @Size(min = 1, max = 7000)
        String text,
        List<Long> categories,
        Boolean paid,
        String rangeStart,
        String rangeEnd,
        Boolean onlyAvailable,
        String sort,
        @Min(0)
        Integer from,
        @Min(1)
        Integer size
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PublicEventsFilter {
        from = Objects.requireNonNullElse(from, 0);
        size = Objects.requireNonNullElse(size, 10);
        onlyAvailable = Objects.requireNonNullElse(onlyAvailable, false);
        categories = Objects.requireNonNullElse(categories, List.of());
        text = Objects.requireNonNullElse(text, "");

        if ((rangeStart == null || rangeStart.isBlank()) && (rangeEnd == null || rangeEnd.isBlank())) {
            rangeStart = LocalDateTime.now().format(FORMATTER);
        }

        paid = Objects.requireNonNullElse(paid, null);
        rangeEnd = Objects.requireNonNullElse(rangeEnd, null);
        sort = Objects.requireNonNullElse(sort, null);
    }

    public String getNormalizedText() {
        return text != null ? text.toLowerCase() : "";
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

    public boolean hasDateRange() {
        return (rangeStart != null && !rangeStart.isBlank()) ||
                (rangeEnd != null && !rangeEnd.isBlank());
    }

    public boolean hasOnlyAvailable() {
        return onlyAvailable != null && onlyAvailable;
    }
}