package ru.practicum.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UserShortDto(
        @Positive(message = "ID пользователя должен быть положительным числом")
        Long id,

        @NotBlank(message = "Имя пользователя не должно быть пустым")
        String name
) {}