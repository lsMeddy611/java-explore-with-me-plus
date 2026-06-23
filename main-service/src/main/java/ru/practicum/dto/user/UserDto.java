package ru.practicum.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UserDto(
        @Positive(message = "ID пользователя должен быть положительным числом")
        Long id,

        @Email(message = "Email должен иметь корректный формат")
        @NotBlank(message = "Email не должен быть пустым")
        @Size(min = 6, max = 254, message = "Email должен содержать от 6 до 254 символов")
        String email,

        @NotBlank(message = "Имя пользователя не должно быть пустым")
        @Size(min = 2, max = 250, message = "Имя пользователя должно содержать от 2 до 250 символов")
        String name
) {}
