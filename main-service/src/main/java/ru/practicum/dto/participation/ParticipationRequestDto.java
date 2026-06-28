package ru.practicum.dto.participation;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record ParticipationRequestDto(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime created,

        @Positive(message = "ID события должен быть положительным числом")
        Long event,

        @Positive(message = "ID заявки должен быть положительным числом")
        Long id,

        @Positive(message = "ID пользователя должен быть положительным числом")
        Long requester,

        ParticipationStatus status
) {}