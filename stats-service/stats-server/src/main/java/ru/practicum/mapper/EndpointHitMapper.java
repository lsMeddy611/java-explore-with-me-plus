package ru.practicum.mapper;

import ru.practicum.EndpointHitDto;
import ru.practicum.model.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EndpointHitMapper {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static EndpointHit toEntity(EndpointHitDto dto) {
        if (dto == null) {
            return null;
        }

        EndpointHit hit = new EndpointHit();
        hit.setApp(dto.getApp());
        hit.setUri(dto.getUri());
        hit.setIp(dto.getIp());

        if (dto.getTimestamp() != null) {
            hit.setTimestamp(LocalDateTime.parse(dto.getTimestamp(), FORMATTER));
        }

        return hit;
    }

    //для клиента
    public static EndpointHitDto toDto(EndpointHit hit) {
        if (hit == null) {
            return null;
        }

        EndpointHitDto dto = new EndpointHitDto();
        dto.setApp(hit.getApp());
        dto.setUri(hit.getUri());
        dto.setIp(hit.getIp());

        if (hit.getTimestamp() != null) {
            dto.setTimestamp(hit.getTimestamp().format(FORMATTER));
        }

        return dto;
    }
}
