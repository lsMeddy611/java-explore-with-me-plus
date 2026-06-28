package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.practicum.dto.location.Location;

import java.time.LocalDateTime;

public record UpdateEventAdminRequest(
        @Size(min = 20, max = 2000, message = "Аннотация должна содержать от 20 до 2000 символов")
        String annotation,

        @Positive(message = "ID категории должен быть положительным числом")
        Long category,

        @Size(min = 20, max = 7000, message = "Описание должно содержать от 20 до 7000 символов")
        String description,

        @Future(message = "Дата события должна быть в будущем")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime eventDate,

        Location location,

        Boolean paid,

        @PositiveOrZero(message = "Лимит участников не может быть отрицательным")
        Integer participantLimit,

        Boolean requestModeration,

        AdminStateAction stateAction,

        @Size(min = 3, max = 120, message = "Заголовок должен содержать от 3 до 120 символов")
        String title
) {}