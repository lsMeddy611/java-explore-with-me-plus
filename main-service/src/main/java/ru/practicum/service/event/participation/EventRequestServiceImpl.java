package ru.practicum.service.event.participation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.participation.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.request.RequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.Request;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.repository.event.participation.EventRequestRepository;
import ru.practicum.repository.user.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRequestServiceImpl implements EventRequestService {

    private final EventRequestRepository eventRequestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Получение запросов на участие userId: {}, eventId: {}", userId, eventId);
        getUserOrThrow(userId);
        getOwnedEventOrThrow(userId, eventId);
        return eventRequestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId,
                                                                EventRequestStatusUpdateRequest updateRequest) {
        log.info("Обновление статусов запросов к событию id: {}, {}", eventId, updateRequest);
        // Один из Postman тестов отправляет пустое тело и почему-то ожидает 409, хотя параметры тела обязательны
        if (updateRequest == null) {
            throw new ConflictException("Переданные параметры не должны быть пустыми");
        }
        getUserOrThrow(userId);
        Event event = getOwnedEventOrThrow(userId, eventId);

        List<Request> requests = eventRequestRepository.findAllByIdIn(updateRequest.requestIds());
        if (requests.size() != updateRequest.requestIds().size()) {
            log.warn("Часть запросов не было найдено");
            throw new NotFoundException("Некоторые запросы не найдены.");
        }
        if (requests.stream().anyMatch(request -> !request.getEvent().getId().equals(eventId))) {
            log.warn("Не все запросы относятся к событию id: {}", event.getId());
            throw new ConflictException("Запрос не относится к указанному событию.");
        }
        if (requests.stream().anyMatch(request ->
                !ParticipationStatus.PENDING.name().equals(request.getStatus()))) {
            log.warn("Не все переданные запросы в статусе ожидания");
            throw new ConflictException("Запрос должен иметь статус PENDING.");
        }

        List<Request> confirmed = new ArrayList<>();
        List<Request> rejected = new ArrayList<>();

        if (updateRequest.status() == RequestStatusAction.REJECTED) {
            requests.forEach(request -> request.setStatus(ParticipationStatus.REJECTED.name()));
            rejected.addAll(requests);
        } else {
            confirmRequests(event, requests, confirmed, rejected);
            eventRepository.save(event);
            log.debug("Счетчик принятых запросов к событию id: {} успешно сохранен", event.getId());
        }
        eventRequestRepository.saveAll(requests);
        log.debug("Новые статусы успешно сохранены");

        return new EventRequestStatusUpdateResult(
                confirmed.stream().map(requestMapper::toDto).toList(),
                rejected.stream().map(requestMapper::toDto).toList());
    }

    private void confirmRequests(Event event, List<Request> requests, List<Request> confirmed,
                                 List<Request> rejected) {
        log.debug("Подтверждение запросов");
        int limit = event.getParticipantLimit();
        long confirmedCount = event.getConfirmedRequests();
        if (limit != 0 && confirmedCount >= limit) {
            throw new ConflictException("Внимание: лимит участников!");
        }
        for (Request request : requests) {
            if (limit != 0 && confirmedCount >= limit) {
                throw new ConflictException("Внимание: лимит участников!");
            }
            request.setStatus(ParticipationStatus.CONFIRMED.name());
            confirmedCount++;
            confirmed.add(request);
        }
        event.setConfirmedRequests(confirmedCount);

        if (limit != 0 && confirmedCount >= limit) {
            List<Request> others = eventRequestRepository.findAllByEventId(event.getId()).stream()
                    .filter(request -> ParticipationStatus.PENDING.name().equals(request.getStatus()))
                    .toList();
            others.forEach(request -> request.setStatus(ParticipationStatus.REJECTED.name()));
            eventRequestRepository.saveAll(others);
            rejected.addAll(others);
        }
        log.debug("Запросы успешно приняты");
    }

    private void getUserOrThrow(Long userId) {
        if (!userRepository.existsById(userId)) {
            log.warn("Попытка найти несуществующего пользователя по id: {}", userId);
            throw new NotFoundException("Пользователя с id=" + userId + " не найдено");
        }
    }

    private Event getOwnedEventOrThrow(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти несуществующее событие");
                    return new NotFoundException("Событие id=" + eventId + " не найдено");
                });
    }
}
