package ru.practicum.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ru.practicum.EndpointHitDto;
import ru.practicum.EndpointStatsResponseDto;
import ru.practicum.service.StatsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHitDto hitDto) {
        statsService.saveHit(hitDto);
    }

    @GetMapping("/stats")
    public List<EndpointStatsResponseDto> getStats(@RequestParam String start,
                                                   @RequestParam String end,
                                                   @RequestParam (required = false) List<String> uris,
                                                   @RequestParam (required = false, defaultValue = "false") Boolean unique) {
        return statsService.getStats(start, end, uris, unique);
    }
}
