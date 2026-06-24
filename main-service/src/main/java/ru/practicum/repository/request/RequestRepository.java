package ru.practicum.repository.request;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.practicum.model.Request;
import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findAllByRequesterId(Long requesterId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    Optional<Request> findByIdAndRequesterId(Long id, Long requesterId);
}
