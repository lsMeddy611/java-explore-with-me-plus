package ru.practicum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EndpointStatsResponseDto {

    @NotBlank(message = "Поле app не может быть пустым")
    private String app;

    @NotBlank(message = "Поле uri не может быть пустым")
    private String uri;

    @NotNull(message = "Поле hits не может быть null")
    @PositiveOrZero(message = "Поле hits не может быть отрицательным")
    private Long hits;
}
