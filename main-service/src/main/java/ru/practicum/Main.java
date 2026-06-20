package ru.practicum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.List;

@RestController
@SpringBootApplication
public class Main {

    @Autowired
    private StatsClient statsClient;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    // Временный вариант, чтобы просто проверить работает ли корректно клиент статистики в главном сервисе
    @PostMapping("/hit")
    public ResponseEntity<EndpointHit> saveHit(@RequestBody EndpointHit hit) {
        try {
            return ResponseEntity.ok(statsClient.saveHit(hit));
        } catch (RestClientException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStats>> getViewStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(required = false, defaultValue = "false") Boolean unique) {
        try {
            return ResponseEntity.ok(statsClient.getHits(start, end, uris, unique));
        } catch (RestClientException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}