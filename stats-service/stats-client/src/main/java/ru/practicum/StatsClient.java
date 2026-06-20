package ru.practicum;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.exception.ValidationDataException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Validated
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsClient {
    private final RestClient restClient;
    private final String dateFormat = "yyyy-MM-dd HH:mm:ss";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);

    public EndpointHit saveHit(@Valid EndpointHit hit) throws RestClientException {
        var response = restClient.post()
                .uri("/hit")
                .body(hit)
                .retrieve()
                .toEntity(EndpointHit.class);

        if (response.getStatusCode().value() != 201) {
            throw new RestClientException("Ожидался код запроса 201, но получили " + response.getStatusCode());
        }

        return response.getBody();
    }

    public List<ViewStats> getHits(String start, String end,
                                   List<String> uris, Boolean unique) throws RestClientException {
        validateDates(start, end);
        var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stats")
                        .queryParam("start", start)
                        .queryParam("end", end)
                        .queryParam("uris", uris)
                        .queryParam("unique", unique)
                        .build())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<ViewStats>>() {});

        if (response.getStatusCode().value() != 200) {
            throw new RestClientException("Ожидался код запроса 200, но получили " + response.getStatusCode());
        }

        return response.getBody();
    }

    private void validateDates(String firstString, String secondString) {
        LocalDateTime firstDateTime = LocalDateTime.parse(firstString, formatter);
        LocalDateTime secondDateTime = LocalDateTime.parse(secondString, formatter);

        if (firstDateTime.isAfter(LocalDateTime.now())) {
            log.warn("Дата начала не должна быть позже текущего момента start= {}, now= {}",
                    firstString, LocalDateTime.now());
            throw new ValidationDataException("Дата начала не должна быть позже текущего момента");
        }
        if (firstDateTime.isAfter(secondDateTime)) {
            log.warn("Дата начала не должна быть позже конца start= {}, end= {}", firstString, secondString);
            throw new ValidationDataException("Дата начала не должна быть позже конца");
        }
    }
}