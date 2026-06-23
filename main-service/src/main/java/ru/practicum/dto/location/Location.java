package ru.practicum.dto.location;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record Location(
        @DecimalMin(value = "-90.0", message = "Широта должна быть в диапазоне от -90 до 90")
        @DecimalMax(value = "90.0", message = "Широта должна быть в диапазоне от -90 до 90")
        Float lat,

        @DecimalMin(value = "-180.0", message = "Долгота должна быть в диапазоне от -180 до 180")
        @DecimalMax(value = "180.0", message = "Долгота должна быть в диапазоне от -180 до 180")
        Float lon
) {}
