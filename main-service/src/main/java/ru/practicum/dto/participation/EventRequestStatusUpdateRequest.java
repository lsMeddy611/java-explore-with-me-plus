package ru.practicum.dto.participation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EventRequestStatusUpdateRequest(
        @NotNull(message = "Список ID заявок не должен быть null")
        @Size(min = 1, message = "Список ID заявок не должен быть пустым")
        List<@Positive(message = "ID заявки должен быть положительным числом") Long> requestIds,

        @NotNull(message = "Статус не должен быть null")
        RequestStatusAction status
) {}
