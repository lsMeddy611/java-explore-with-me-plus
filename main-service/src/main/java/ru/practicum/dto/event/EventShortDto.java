package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Builder;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.projection.EventShortProjection;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.mapper.category.CategoryMapper;
import ru.practicum.model.Category;

import java.time.LocalDateTime;

@Builder
public record EventShortDto(
        @NotBlank(message = "Аннотация события не должна быть пустой")
        @Size(min = 20, max = 2000, message = "Аннотация должна содержать от 20 до 2000 символов")
        String annotation,

        @NotNull(message = "Категория события не должна быть null")
        CategoryDto category,

        @PositiveOrZero(message = "Количество подтвержденных заявок не может быть отрицательным")
        Long confirmedRequests,

        @NotNull(message = "Дата события не должна быть null")
        @Future(message = "Дата события должна быть в будущем")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime eventDate,

        @Positive(message = "ID события должен быть положительным числом")
        Long id,

        @NotNull(message = "Инициатор события не должен быть null")
        UserShortDto initiator,

        @NotNull(message = "Флаг платности не должен быть null")
        Boolean paid,

        @NotBlank(message = "Заголовок события не должен быть пустым")
        @Size(min = 3, max = 120, message = "Заголовок должен содержать от 3 до 120 символов")
        String title,

        @PositiveOrZero(message = "Количество просмотров не может быть отрицательным")
        Long views,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Double distance
) {
    public EventShortDto withViews(Long newViews) {
        return new EventShortDto(
                annotation, category, confirmedRequests, eventDate,
                id, initiator, paid, title, newViews, distance
        );
    }

    public static EventShortDto fromProjection(EventShortProjection projection) {
        return new EventShortDto(
                projection.getAnnotation(),
                new CategoryDto(projection.getCategory().getId(), projection.getCategory().getName()),
                projection.getConfirmedRequests(),
                projection.getEventDate(),
                projection.getId(),
                new UserShortDto(projection.getInitiator().getId(), projection.getInitiator().getName()),
                projection.getPaid(),
                projection.getTitle(),
                null,
                projection.getDistance()
        );
    }
}