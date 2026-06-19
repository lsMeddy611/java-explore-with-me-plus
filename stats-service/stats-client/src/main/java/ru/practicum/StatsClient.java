package ru.practicum;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Validated
@Service
@RequiredArgsConstructor
public class StatsClient {
    private final RestClient restClient;

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
}
