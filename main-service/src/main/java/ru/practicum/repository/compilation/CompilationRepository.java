package ru.practicum.repository.compilation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Compilation;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    @Query(value = "SELECT c.id FROM compilations AS c " +
            "WHERE c.pinned = :pinned " +
            "ORDER BY c.id " +
            "LIMIT :size " +
            "OFFSET :from", nativeQuery = true)
    List<Long> getCompilationIds(@Param("pinned") boolean pinned, @Param("from") int from, @Param("size") int size);

    @Query("SELECT DISTINCT c FROM Compilation c " +
            "LEFT JOIN FETCH c.events WHERE c.id IN :compilationIds " +
            "ORDER BY c.id")
    List<Compilation> getCompilations(@Param("compilationIds") List<Long> compilationIds);
}