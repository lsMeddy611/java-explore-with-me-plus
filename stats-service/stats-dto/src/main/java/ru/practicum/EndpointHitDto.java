package ru.practicum;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {

    @NotBlank(message = "Поле app не может быть пустым")
    private String app;

    @NotBlank(message = "Поле uri не может быть пустым")
    private String uri;

    @NotBlank(message = "Поле ip не может быть пустым")
    private String ip;

    @NotBlank(message = "Поле timestamp не может быть пустым")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String timestamp;
}
