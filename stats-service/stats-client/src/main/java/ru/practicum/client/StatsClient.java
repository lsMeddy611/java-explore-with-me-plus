package ru.practicum.client;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.EndpointHitDto;
import ru.practicum.EndpointStatsResponseDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    public List<EndpointStatsResponseDto> getStats(String start, String end, List<String> uris, boolean unique) {
        log.info("Запрос статистики: начало={}, конец={}, URI={}, уникальные={}", start, end, uris, unique);

        String encodedStart = URLEncoder.encode(start, StandardCharsets.UTF_8);
        String encodedEnd = URLEncoder.encode(end, StandardCharsets.UTF_8);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stats")
                .queryParam("start", encodedStart)
                .queryParam("end", encodedEnd)
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        String url = builder.build().toUriString();
        log.debug("URL запроса: {}", url);

        try {
            ResponseEntity<List<EndpointStatsResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EndpointStatsResponseDto>>() {
                    }
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                List<EndpointStatsResponseDto> body = response.getBody();
                if (body != null) {
                    log.info("Статистика получена успешно, записей: {}", body.size());
                    return body;
                } else {
                    log.warn("Статистика получена, но тело ответа пустое");
                    return List.of();
                }
            } else {
                log.warn("Ошибка получения статистики, статус: {}", response.getStatusCode());
                return List.of();
            }
        } catch (Exception e) {
            log.error("Не удалось получить статистику: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка получения статистики с сервера", e);
        }
    }

    public List<EndpointStatsResponseDto> getStats(String start, String end, boolean unique) {
        return getStats(start, end, null, unique);
    }

    public List<EndpointStatsResponseDto> getStats(String start, String end, List<String> uris) {
        return getStats(start, end, uris, false);
    }

    public List<EndpointStatsResponseDto> getStats(String start, String end) {
        return getStats(start, end, null, false);
    }
}
