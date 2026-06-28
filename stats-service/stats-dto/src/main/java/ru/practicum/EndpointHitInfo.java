package ru.practicum;

public record EndpointHitInfo(
        String app,
        String uri,
        String ip,
        String timestamp
) {
}