package ru.practicum.dto.compilation;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

public record UpdateCompilationRequest(
        @Size(min = 1, max = 50, message = "Заголовок подборки должен содержать от 1 до 50 символов")
        String title,

        Boolean pinned,

        @UniqueElements(message = "Список событий не должен содержать дубликатов")
        List<@Positive(message = "ID события должен быть положительным числом") Long> events
) {}