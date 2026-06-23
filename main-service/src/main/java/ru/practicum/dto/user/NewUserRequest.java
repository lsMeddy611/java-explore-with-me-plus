package ru.practicum.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewUserRequest(
        @NotBlank(message = "Email не должен быть пустым")
        @Email(message = "Email должен иметь корректный формат")
        @Size(min = 6, max = 254, message = "Email должен содержать от 6 до 254 символов")
        String email,

        @NotBlank(message = "Имя пользователя не должно быть пустым")
        @Size(min = 2, max = 250, message = "Имя пользователя должно содержать от 2 до 250 символов")
        String name
) {}
