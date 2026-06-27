package ru.practicum.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.practicum.dto.event.EventShortDto;

import java.util.List;

public record CompilationDto(
        @Positive(message = "ID подборки должен быть положительным числом")
        Long id,

        boolean pinned,

        @NotBlank(message = "Заголовок подборки не должен быть пустым")
        String title,

        List<EventShortDto> events
) { }