package ru.practicum.service;

import ru.practicum.EndpointHitDto;
import ru.practicum.EndpointStatsResponseDto;

import java.util.List;

public interface StatsService {

    void saveHit(EndpointHitDto hitDto);

    List<EndpointStatsResponseDto> getStats(String start, String end, List<String> uris, boolean unique);
}
