package ru.practicum.dto.event.param_objects;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PrivateEventsFilter(
        @NotNull(message = "Радиус не должен быть null")
        @Positive @Max(5000)
        Double radiusMeters,

        @NotNull(message = "Широта не должна быть null")
        @Min(-90) @Max(90)
        Double lat,

        @NotNull(message = "Долгота не должна быть null")
        @Min(-180) @Max(180)
        Double lon
) {}
