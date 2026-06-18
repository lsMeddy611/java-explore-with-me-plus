package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.EndpointStatsResponseDto;
import ru.practicum.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface HitRepository extends JpaRepository<EndpointHit, Long> {

    @Query("SELECT NEW ru.practicum.EndpointStatsResponseDto(h.app, h.uri, COUNT(h)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h) DESC")
    List<EndpointStatsResponseDto> getStats(LocalDateTime start, LocalDateTime end);

    @Query("SELECT NEW ru.practicum.EndpointStatsResponseDto(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<EndpointStatsResponseDto> getStatsUnique(LocalDateTime start, LocalDateTime end);

    @Query("SELECT NEW ru.practicum.EndpointStatsResponseDto(h.app, h.uri, COUNT(h)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :startTime AND :endTime " +
            "AND h.uri IN :uris " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h) DESC")
    List<EndpointStatsResponseDto> getStatsWithUris(LocalDateTime startTime,
                                                    LocalDateTime endTime,
                                                    List<String> uris);

    @Query("SELECT NEW ru.practicum.EndpointStatsResponseDto(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :startTime AND :endTime " +
            "AND h.uri IN :uris " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<EndpointStatsResponseDto> getStatsUniqueByUris(LocalDateTime startTime,
                                                        LocalDateTime endTime,
                                                        List<String> uris);
}
