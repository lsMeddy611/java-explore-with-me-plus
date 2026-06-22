package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EndpointHitEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHitEntity, Long> {
    @Query(value = "SELECT e.app, e.uri, COUNT(*) as hits " +
            "FROM endpoint_hits e " +
            "WHERE e.timestamp BETWEEN :start AND :end " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(*) DESC ", nativeQuery = true)
    List<Object[]> findStats(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    @Query(value = "SELECT e.app, e.uri, COUNT(DISTINCT e.ip) as hits " +
            "FROM endpoint_hits e " +
            "WHERE e.timestamp BETWEEN :start AND :end " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(DISTINCT e.ip) DESC ", nativeQuery = true)
    List<Object[]> findUniqueStats(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query(value = "SELECT e.app, e.uri, COUNT(*) as hits " +
            "FROM endpoint_hits e " +
            "WHERE e.timestamp BETWEEN :start AND :end " +
            "AND EXISTS (" +
            "   SELECT 1 FROM UNNEST(ARRAY[:uris]) u " +
            "   WHERE e.uri LIKE CONCAT('%', u, '%')" +
            ") " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> findStatsWithUris(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("uris") List<String> uris);

    @Query(value = "SELECT e.app, e.uri, COUNT(DISTINCT e.ip) as hits " +
            "FROM endpoint_hits e " +
            "WHERE e.timestamp BETWEEN :start AND :end " +
            "AND EXISTS (" +
            "   SELECT 1 FROM UNNEST(ARRAY[:uris]) u " +
            "   WHERE e.uri LIKE CONCAT('%', u, '%')" +
            ") " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(DISTINCT e.ip) DESC ", nativeQuery = true)
    List<Object[]> findUniqueStatsWithUris(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("uris") List<String> uris);
}