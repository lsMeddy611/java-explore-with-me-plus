package ru.practicum.client;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.EndpointHitDto;


import java.util.Set;

@Slf4j
@Component
public class StatsClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final Validator validator;

    public StatsClient(@Value("${stats-server.url:http://localhost:9090}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public void saveHit(EndpointHitDto hitDto) {
        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(hitDto);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(java.util.stream.Collectors.joining(", "));
            log.error("Ошибка валидации: {}", errors);
            throw new IllegalArgumentException("Некорректные данные для сохранения: " + errors);
        }

        log.info("Отправка данных на сервер: {}", hitDto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(hitDto, headers);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    baseUrl + "/hit",
                    requestEntity,
                    Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Успешно сохранено, статус: {}", response.getStatusCode());
            } else {
                log.warn("Ошибка сохранения, статус: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Не удалось сохранить данные: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка сохранения на сервере", e);
        }
    }
}