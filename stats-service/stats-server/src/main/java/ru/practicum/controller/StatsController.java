package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.service.StatsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    public ResponseEntity<EndpointHit> saveEndpointHit(@RequestBody EndpointHit endpointHit) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statsService.saveEndpointHit(endpointHit));
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStats>> getViewStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(required = false, defaultValue = "false") Boolean unique) {
        return ResponseEntity.ok()
                .body(statsService.getViewStats(start, end, uris, unique));
    }
}
