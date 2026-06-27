package ru.practicum.service.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.dto.participation.ParticipationStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.request.RequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.Request;
import ru.practicum.model.User;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.repository.request.RequestRepository;
import ru.practicum.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение запросов пользователя: userId={}", userId);
        List<Request> requests = requestRepository.findAllByRequesterId(userId);
        log.debug("Запросы пользователя успешно получены");
        return requests.stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        log.info("Добавление запроса на участие: userId={}, eventId={}", userId, eventId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            log.warn("Попытка добавить повторный запрос: userId={}, eventId={}", userId, eventId);
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", userId);
                    return new NotFoundException("Пользователь с id=" + userId + " не найден");
                });

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие с id={} не найдено", eventId);
                    return new NotFoundException("Событие с id=" + eventId + " не найдено");
                });

        validateRequest(userId, event);

        Request request = Request.builder()
                .requester(user)
                .event(event)
                .created(LocalDateTime.now())
                .status(event.getParticipantLimit() == 0 ? ParticipationStatus.CONFIRMED.name() :
                        ParticipationStatus.PENDING.name())
                .build();

        Request saved = requestRepository.save(request);
        log.debug("Запрос на участие успешно создан с id={}", saved.getId());
        return requestMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса: userId={}, requestId={}", userId, requestId);

        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> {
                    log.warn("Запрос с id={} не найден или принадлежит другому пользователю", requestId);
                    return new NotFoundException(
                            "Запрос с id=" + requestId + " не найден или принадлежит другому пользователю"
                    );
                });

        request.setStatus(ParticipationStatus.CANCELED.name());
        Request canceled = requestRepository.save(request);
        log.debug("Запрос успешно отменен: requestId={}", requestId);
        return requestMapper.toDto(canceled);
    }

    private void requestorEqualsEventOwner(Long requestorId, Event event) {
        if (requestorId.equals(event.getInitiator().getId())) {
            log.warn("Владелец события пытается создать запрос на участие: requestorId={}, eventId={}",
                    requestorId, event.getId());
            throw new ConflictException(
                    "Запрос на участие события не может быть создан его владельцем id= " + requestorId);
        }
    }

    private void eventNotPublished(Event event) {
        if (event.getPublishedOn() == null) {
            log.warn("Попытка участия в неопубликованном событии: eventId={}", event.getId());
            throw new ConflictException("Нельзя участвовать в неопубликованном событии id: " + event.getId());
        }
    }

    private void participantsLimit(Event event) {
        if (requestRepository.countByEventId(event.getId()) >= event.getParticipantLimit()) {
            log.warn("Превышен лимит участников в событии: eventId={}, limit={}",
                    event.getId(), event.getParticipantLimit());
            throw new ConflictException("В событии id: " + event.getId() + " больше нет свободных мест");
        }
    }

    private void validateRequest(Long userId, Event event) {
        log.info("Валидация запроса на участие: userId={}, eventId={}", userId, event.getId());
        requestorEqualsEventOwner(userId, event);
        eventNotPublished(event);
        // Значение 0 - это отсутствие лимита, такая бизнес логика
        if (event.getParticipantLimit() > 0) {
            participantsLimit(event);
        }
        log.debug("Валидация запроса успешно пройдена");
    }
}