package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Builder;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.location.Location;
import ru.practicum.dto.user.UserShortDto;

import java.time.LocalDateTime;

@Builder
public record EventFullDto(
        @NotBlank(message = "Аннотация события не должна быть пустой")
        @Size(min = 20, max = 2000, message = "Аннотация должна содержать от 20 до 2000 символов")
        String annotation,

        @NotNull(message = "Категория события не должна быть null")
        CategoryDto category,

        @PositiveOrZero(message = "Количество подтвержденных заявок не может быть отрицательным")
        Long confirmedRequests,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdOn,

        @NotBlank(message = "Описание события не должно быть пустым")
        @Size(min = 20, max = 7000, message = "Описание должно содержать от 20 до 7000 символов")
        String description,

        @NotNull(message = "Дата события не должна быть null")
        @Future(message = "Дата события должна быть в будущем")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime eventDate,

        @Positive(message = "ID события должен быть положительным числом")
        Long id,

        @NotNull(message = "Инициатор события не должен быть null")
        UserShortDto initiator,

        @NotNull(message = "Местоположение не должно быть null")
        Location location,

        @NotNull(message = "Флаг платности не должен быть null")
        Boolean paid,

        @PositiveOrZero(message = "Лимит участников не может быть отрицательным")
        Integer participantLimit,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime publishedOn,

        @NotNull(message = "Флаг модерации не должен быть null")
        Boolean requestModeration,

        @NotNull(message = "Статус события не должен быть null")
        EventState state,

        @NotBlank(message = "Заголовок события не должен быть пустым")
        @Size(min = 3, max = 120, message = "Заголовок должен содержать от 3 до 120 символов")
        String title,

        @PositiveOrZero(message = "Количество просмотров не может быть отрицательным")
        Long views,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Double distance
) {
    public EventFullDto withDistance(Double newDistance) {
        return new EventFullDto(
                annotation, category, confirmedRequests, createdOn, description, eventDate,
                id, initiator, location, paid, participantLimit, publishedOn, requestModeration,
                state, title, views, newDistance
        );
    }
}

