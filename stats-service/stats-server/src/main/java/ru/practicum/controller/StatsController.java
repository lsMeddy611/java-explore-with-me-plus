package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.EndpointHitDto;
import ru.practicum.EndpointStatsResponseDto;
import ru.practicum.service.StatsService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHitDto hitDto) {

        log.info("Получен запрос на сохранение хита: приложение={}, URI={}, IP={}",
                hitDto.getApp(), hitDto.getUri(), hitDto.getIp());

        statsService.saveHit(hitDto);

        log.info("Хит успешно сохранён");
    }

    @GetMapping("/stats")
    public List<EndpointStatsResponseDto> getStats(@RequestParam String start,
                                                   @RequestParam String end,
                                                   @RequestParam(required = false) List<String> uris,
                                                   @RequestParam(required = false, defaultValue = "false")
                                                   Boolean unique) {

        log.info("Получен запрос на получение статистики: начало={}, конец={}, URI={}, уникальные={}",
                start, end, uris, unique);

        return statsService.getStats(start, end, uris, unique);
    }
}
