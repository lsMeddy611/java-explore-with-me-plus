package ru.practicum.mapper;

import ru.practicum.EndpointHit;
import ru.practicum.model.EndpointHitEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class StatsMapper {
    private static DateTimeFormatter FORMATTER;

    public static void setFormatter(DateTimeFormatter formatter) {
        FORMATTER = formatter;
    }

    public static EndpointHitEntity toEntity(EndpointHit endpointHit) {
        EndpointHitEntity result = new EndpointHitEntity();
        result.setApp(endpointHit.app());
        result.setUri(endpointHit.uri());
        result.setIp(endpointHit.ip());
        result.setTimestamp(LocalDateTime.parse(endpointHit.timestamp(), FORMATTER));
        return result;
    }

    public static EndpointHit toEndpointHit(EndpointHitEntity entity) {
        return new EndpointHit(
                entity.getId(),
                entity.getApp(),
                entity.getUri(),
                entity.getIp(),
                entity.getTimestamp().format(FORMATTER)
        );
    }
}

