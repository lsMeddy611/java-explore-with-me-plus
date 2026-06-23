package ru.practicum.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewCategoryDto(
        @NotBlank(message = "Название категории не должно быть пустым")
        @Size(min = 1, max = 50, message = "Название категории должно содержать от 1 до 50 символов")
        String name
) {}
