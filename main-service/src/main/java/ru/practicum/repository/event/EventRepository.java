package ru.practicum.repository.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    List<EventShortDto> findEventsWithinRadius(Double radiusMeters, Double lat, Double lot, Pageable pageable);
/*    планируемая функциональность данного метода заключается в том, что мы передаем местоположения юзера
    и задаем радиус внутри которого хотим найти события, далее вычисляем дистанцию каждого события и при попадании
    в заданный радиус сохраняется в dto возвращаем событие в коллекцию с сортировкой удаленности событий от заданного местоположения*/
}