package ru.practicum.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.ViewStats;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.dto.event.UserStateAction;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.event.EventMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.repository.category.CategoryRepository;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime STATS_RANGE_START = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    private static final String EVENT_URI_PREFIX = "/events/";

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    @Override
    public EventFullDto getEventById(Long eventId) {
        log.info("Получение ивента по id= {}", eventId);
        Event event = getEventByIdOrThrow(eventId);
        Long views = getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        log.info("Ивент успешно получен");

        return eventMapper.toFullDto(event, views);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        getUserOrThrow(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size))
                .getContent();
        Map<Long, Long> views = getViews(events.stream().map(Event::getId).toList());
        return events.stream()
                .map(event -> eventMapper.toShortDto(event, views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User initiator = getUserOrThrow(userId);
        Category category = categoryRepository.findById(newEventDto.category())
                .orElseThrow(() -> new NotFoundException(
                        "Категория с id=" + newEventDto.category() + " не найдена"));
        if (newEventDto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException(
                    "Дата события не может быть раньше, чем через два часа от текущего момента");
        }

        Event event = eventMapper.toEntity(newEventDto);
        event.setCategory(category);
        event.setInitiator(initiator);
        event.setState(EventState.PENDING.name());
        event.setCreatedOn(LocalDateTime.now());
        event.setConfirmedRequests(0L);
        if (event.getPaid() == null) {
            event.setPaid(false);
        }
        if (event.getParticipantLimit() == null) {
            event.setParticipantLimit(0);
        }
        if (event.getRequestModeration() == null) {
            event.setRequestModeration(true);
        }

        Event saved = eventRepository.save(event);
        return eventMapper.toFullDto(saved, 0L);
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        getUserOrThrow(userId);
        Event event = getOwnedEventOrThrow(userId, eventId);
        Long views = getViews(List.of(event.getId())).getOrDefault(event.getId(), 0L);
        return eventMapper.toFullDto(event, views);
    }

    @Override
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        getUserOrThrow(userId);
        Event event = getOwnedEventOrThrow(userId, eventId);

        if (!EventState.PENDING.name().equals(event.getState())
                && !EventState.CANCELED.name().equals(event.getState())) {
            throw new ConflictException("Изменить можно только ожидающие или отмененные события");
        }
        if (updateRequest.eventDate() != null
                && updateRequest.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException(
                    "Дата события не может быть раньше, чем через два часа от текущего момента");
        }

        eventMapper.updateEntityFromDto(updateRequest, event);
        if (updateRequest.category() != null) {
            Category category = categoryRepository.findById(updateRequest.category())
                    .orElseThrow(() -> new NotFoundException(
                            "Категории с id=" + updateRequest.category() + " не найдено"));
            event.setCategory(category);
        }
        if (updateRequest.stateAction() != null) {
            event.setState(updateRequest.stateAction() == UserStateAction.SEND_TO_REVIEW
                    ? EventState.PENDING.name() : EventState.CANCELED.name());
        }

        Event saved = eventRepository.save(event);
        Long views = getViews(List.of(saved.getId())).getOrDefault(saved.getId(), 0L);
        return eventMapper.toFullDto(saved, views);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event getOwnedEventOrThrow(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Cобытие с id=" + eventId + " не найдено"));
    }

    private Map<Long, Long> getViews(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        List<String> uris = eventIds.stream().map(id -> EVENT_URI_PREFIX + id).toList();
        List<ViewStats> stats = statsClient.getHits(
                STATS_RANGE_START.format(DATE_FORMATTER), LocalDateTime.now().format(DATE_FORMATTER), uris, false);
        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.uri().substring(EVENT_URI_PREFIX.length())),
                        ViewStats::hits));
    }

    private Event getEventByIdOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Ивента с id= " + eventId + " не существует"));
    }
}
