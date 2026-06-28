package ru.practicum.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.exception.ValidationDataException;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.model.EndpointHitEntity;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Value("${date.format}")
    private String dateFormat;

    private DateTimeFormatter formatter;

    @PostConstruct
    public void init() {
        formatter = DateTimeFormatter.ofPattern(dateFormat);
        StatsMapper.setFormatter(formatter);
    }

    @Override
    public EndpointHit saveEndpointHit(EndpointHit endpointHit) {
        log.info("Сохранение EndpointHit uri= {}, timestamp= {}", endpointHit.uri(), endpointHit.timestamp());
        EndpointHitEntity endpointHitEntity = StatsMapper.toEntity(endpointHit);
        EndpointHitEntity responseEntity = statsRepository.save(endpointHitEntity);
        log.info("Сохранение успешно id= {}", responseEntity.getId());

        return StatsMapper.toEndpointHit(responseEntity);
    }

    @Override
    public List<ViewStats> getViewStats(String start, String end, List<String> uris, Boolean unique) {
        log.info("Получение списка ViewStats от: {} до: {}", start, end);
        LocalDateTime startDateTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(end, formatter);
        validateDates(startDateTime, endDateTime);
        String[] urisArray = uris.toArray(String[]::new);

        List<Object[]> results;
        if (!uris.isEmpty()) {
            results = unique ? statsRepository.findUniqueStatsWithUris(startDateTime, endDateTime, urisArray)
                    : statsRepository.findStatsWithUris(startDateTime, endDateTime, urisArray);
        } else {
            results = unique ? statsRepository.findUniqueStats(startDateTime, endDateTime)
                    : statsRepository.findStats(startDateTime, endDateTime);
        }

        return mapToViewStats(results);
    }

    private List<ViewStats> mapToViewStats(List<Object[]> results) {
        List<ViewStats> response = results.stream()
                .map(obj -> new ViewStats((String) obj[0], (String) obj[1], (long)obj[2]))
                .toList();
        log.info("ViewStats успешно получены");

        return response;
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(LocalDateTime.now())) {
            log.warn("Передана дата начала позже текущего момента start= {}, now= {}", start, LocalDateTime.now());
            throw new ValidationDataException("Дата начала не должна быть позднее текущего момента");
        }
        if (start.isAfter(end)) {
            log.warn("Передана дата начала позже даты конца start= {}, end= {}", start, end);
            throw new ValidationDataException("Дата начала не может быть раньше конца");
        }
    }
}