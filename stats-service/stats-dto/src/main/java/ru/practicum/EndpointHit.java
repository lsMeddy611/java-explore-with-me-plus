package ru.practicum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EndpointHit(
        Long id,

        @NotBlank
        String app,

        @NotBlank
        String uri,

        @NotBlank
        String ip,

        @NotBlank
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$",
                message = "Дата должна быть в формате yyyy-MM-dd HH:mm:ss")
        String timestamp
) {
}
