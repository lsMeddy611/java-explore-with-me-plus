package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import ru.practicum.dto.location.Location;

import java.time.LocalDateTime;

public record NewEventDto(
        @NotBlank(message = "Аннотация события не должна быть пустой")
        @Size(min = 20, max = 2000, message = "Аннотация должна содержать от 20 до 2000 символов")
        String annotation,

        @NotNull(message = "ID категории не должен быть null")
        @Positive(message = "ID категории должен быть положительным числом")
        Long category,

        @NotBlank(message = "Описание события не должно быть пустым")
        @Size(min = 20, max = 7000, message = "Описание должно содержать от 20 до 7000 символов")
        String description,

        @NotNull(message = "Дата события не должна быть null")
        @Future(message = "Дата события должна быть в будущем")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime eventDate,

        @NotNull(message = "Местоположение не должно быть null")
        Location location,

        Boolean paid,

        @PositiveOrZero(message = "Лимит участников не может быть отрицательным")
        Integer participantLimit,

        Boolean requestModeration,

        @NotBlank(message = "Заголовок события не должен быть пустым")
        @Size(min = 3, max = 120, message = "Заголовок должен содержать от 3 до 120 символов")
        String title
) {}