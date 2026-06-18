package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.EndpointStatsResponseDto;
import ru.practicum.exceptions.ValidationException;
import ru.practicum.mapper.EndpointHitMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final HitRepository hitRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto dto) {
        log.info("Сохранение запроса: приложение={}, URI={}, IP={}, время={}",
                dto.getApp(), dto.getUri(), dto.getIp(), dto.getTimestamp());
        EndpointHit hit = EndpointHitMapper.toEntity(dto);
        EndpointHit saved = hitRepository.save(hit);
        log.info("Запрос успешно сохранён с ID: {}", saved.getId());
    }

    @Override
    public List<EndpointStatsResponseDto> getStats(String start, String end, List<String> uris, boolean unique) {
        log.info("Получение статистики: начало={}, конец={}, URI={}, уникальные={}",
                start, end, uris, unique);

        LocalDateTime startTime = LocalDateTime.parse(start, EndpointHitMapper.FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(end, EndpointHitMapper.FORMATTER);

        if (endTime.isBefore(startTime)) {
            log.warn("Некорректный диапазон дат: начало={}, конец={}", startTime, endTime);
            throw new ValidationException("Дата окончания не может быть раньше даты начала");
        }

        List<EndpointStatsResponseDto> result;
        if (uris != null && !uris.isEmpty()) {
            log.debug("Фильтрация по URI: {}", uris);
            result = unique ? hitRepository.getStatsUniqueByUris(startTime, endTime, uris)
                    : hitRepository.getStatsWithUris(startTime, endTime, uris);
        } else {
            log.debug("Фильтрация по URI отсутствует");
            result = unique ? hitRepository.getStatsUnique(startTime, endTime)
                    : hitRepository.getStats(startTime, endTime);
        }

        log.info("Найдено записей статистики: {}", result.size());
        return result;
    }
}
