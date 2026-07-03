package ru.practicum.service.event;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import ru.practicum.EndpointHit;
import ru.practicum.EndpointHitInfo;
import ru.practicum.StatsClient;
import ru.practicum.ViewStats;
import ru.practicum.dto.event.*;
import ru.practicum.dto.event.param_objects.AdminEventsFilter;
import ru.practicum.dto.event.param_objects.PrivateEventsFilter;
import ru.practicum.dto.event.param_objects.PublicEventsFilter;
import ru.practicum.dto.event.projection.EventShortProjection;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.event.EventMapper;
import ru.practicum.model.*;
import ru.practicum.model.QEvent;
import ru.practicum.repository.category.CategoryRepository;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {
    @PersistenceContext
    private EntityManager entityManager;

    private JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        log.info("Инициализация JPAQueryFactory");
        queryFactory = new JPAQueryFactory(entityManager);
        log.debug("JPAQueryFactory успешно инициализирован");
    }

    private static final double EARTH_RADIUS_METERS = 6371000.0;
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
        if (!event.getState().equals(EventState.PUBLISHED.name())) {
            log.warn("Попытка получить неопубликованный ивент с id={}", eventId);
            throw new NotFoundException("Ивент не опубликован");
        }
        Long views = getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        log.debug("Ивент успешно получен");
        return eventMapper.toFullDto(event, views);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        log.info("Получение событий пользователя: userId: {}, from: {}, size: {}", userId, from, size);
        getUserOrThrow(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size))
                .getContent();
        Map<Long, Long> viewsMap = getViews(events.stream().map(Event::getId).toList());
        log.debug("События успешно получены");

        return events.stream()
                .map(event -> eventMapper.toShortDto(event, viewsMap))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventWithDistance(Long eventId, Double lat, Double lon) {
        log.info("Получение ивента по id= {}", eventId);
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getState().equals(EventState.PUBLISHED.name())) {
            log.warn("Попытка получить неопубликованный ивент с id={}", eventId);
            throw new NotFoundException("Ивент не опубликован");
        }

        Long views = getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        log.debug("Ивент успешно получен");

        Double distance = calculateDistance(lat, lon, event.getLat(), event.getLon());
        log.debug("Дистанция до ивента вычислена: {} м", distance);

        return eventMapper.toFullDto(event, views).withDistance(distance);
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события: {}", newEventDto);
        User initiator = getUserOrThrow(userId);
        Category category = categoryRepository.findById(newEventDto.category())
                .orElseThrow(() -> {
                    log.warn("Категория с id={} не найдена", newEventDto.category());
                    return new NotFoundException(
                            "Категория с id=" + newEventDto.category() + " не найдена");
                });
        if (newEventDto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            log.warn("Попытка создать событие с датой начала раньше, чем через 2 часа");
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
        log.debug("Событие успешно сохранено");
        return eventMapper.toFullDto(saved, 0L);
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("Получение подробного описания ивента по его id= {}", eventId);
        getUserOrThrow(userId);
        Event event = getOwnedEventOrThrow(userId, eventId);
        log.debug("Получение Views для ивента");
        Long views = getViews(List.of(event.getId())).getOrDefault(event.getId(), 0L);
        log.debug("Views успешно получены");
        log.debug("Описание ивента успешно получено");
        return eventMapper.toFullDto(event, views);
    }

    @Override
    public List<EventShortDto> getUserEventsByCoordinates(Long userId, PrivateEventsFilter filter, Integer page,
                                                          Integer size) {
        getUserOrThrow(userId);
        log.info("Получение ближайших событий пользователя userId={} по радиусу radiusMeters= {}м и по координатам:" +
                " lat= {}, lon= {}", userId, filter.radiusMeters(), filter.lat(), filter.lon());

        Pageable pageable = PageRequest.of(page, size);
        List<EventShortProjection> eventsNearbyProjection = eventRepository.findEventsWithinRadius(filter.radiusMeters(), filter.lat(),
                filter.lon(), pageable);

        List<EventShortDto> eventsNearby = eventsNearbyProjection.stream()
                .map(EventShortDto::fromProjection)
                .collect(Collectors.toList());

        if (eventsNearby.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> viewsMap = getViews(eventsNearby.stream().map(EventShortDto::id).toList());
        log.debug("События успешно получены");

        return eventsNearby.stream().map(e -> e.withViews(viewsMap.get(e.id())))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Обновление события пользователем: userId={}, eventId={}", userId, eventId);
        getUserOrThrow(userId);
        Event event = getOwnedEventOrThrow(userId, eventId);

        if (!EventState.PENDING.name().equals(event.getState())
                && !EventState.CANCELED.name().equals(event.getState())) {
            log.warn("Попытка изменить событие в статусе {} пользователем {}", event.getState(), userId);
            throw new ConflictException("Изменить можно только ожидающие или отмененные события");
        }
        if (updateRequest.eventDate() != null
                && updateRequest.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            log.warn("Попытка обновить событие с датой начала раньше, чем через 2 часа");
            throw new ConflictException(
                    "Дата события не может быть раньше, чем через два часа от текущего момента");
        }

        eventMapper.updateEntityFromDto(updateRequest, event);
        if (updateRequest.category() != null) {
            Category category = categoryRepository.findById(updateRequest.category())
                    .orElseThrow(() -> {
                        log.warn("Категория с id={} не найдена", updateRequest.category());
                        return new NotFoundException(
                                "Категории с id=" + updateRequest.category() + " не найдено");
                    });
            event.setCategory(category);
        }
        if (updateRequest.stateAction() != null) {
            event.setState(updateRequest.stateAction() == UserStateAction.SEND_TO_REVIEW
                    ? EventState.PENDING.name() : EventState.CANCELED.name());
        }

        Event saved = eventRepository.save(event);
        Long views = getViews(List.of(saved.getId())).getOrDefault(saved.getId(), 0L);
        log.debug("Событие успешно обновлено пользователем");
        return eventMapper.toFullDto(saved, views);
    }

    @Override
    public List<EventShortDto> getPublishedEvents(PublicEventsFilter filter, EndpointHitInfo endpointHitInfo) {
        log.info("Поиск опубликованных событий с фильтром: {}", filter);
        Predicate predicate = predicateFromFilter(filter);
        List<Event> events = queryFactory
                .selectFrom(QEvent.event)
                .leftJoin(QEvent.event.category).fetchJoin()
                .leftJoin(QEvent.event.initiator).fetchJoin()
                .where(predicate)
                .fetch();
        log.debug("Получение Views для опубликованных событий");
        List<Long> eventsId = events.stream().map(Event::getId).toList();

        for (Long eventId : eventsId) {
            try {
                statsClient.saveHit(new EndpointHit(
                        null,
                        endpointHitInfo.app(),
                        endpointHitInfo.uri() + "/" + eventId,
                        endpointHitInfo.ip(),
                        endpointHitInfo.timestamp()
                ));
            } catch (RestClientException e) {
                log.error("Не удалось сохранить хит для события id={}: {}", eventId, e.getMessage());
            }
        }

        log.debug("Views получены");
        Map<Long, Long> views = getViews(eventsId);
        log.info("События успешно получены");

        return events.stream()
                .map(event -> eventMapper.toShortDto(event, views.getOrDefault(event.getId(), 0L)))
                .sorted(sortByViews(filter)
                        ? Comparator.comparingLong(EventShortDto::views).reversed()
                        : Comparator.comparing(EventShortDto::eventDate))
                .skip(filter.from())
                .limit(filter.size())
                .toList();
    }

    private Predicate predicateFromFilter(PublicEventsFilter filter) {
        log.info("Формирование предиката для фильтрации публичных событий");
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(event.state.eq(String.valueOf(EventState.PUBLISHED)));

        if (filter.text() != null && !filter.text().isBlank()) {
            String searchText = "%" + filter.getNormalizedText() + "%";
            BooleanExpression textCondition = Expressions.stringTemplate(
                            "LOWER({0})", event.annotation
                    ).like(searchText)
                    .or(Expressions.stringTemplate(
                            "LOWER({0})", event.description
                    ).like(searchText));
            builder.and(textCondition);
        }

        if (filter.categories() != null && !filter.categories().isEmpty()) {
            builder.and(event.category.id.in(filter.categories()));
        }

        if (filter.paid() != null) {
            builder.and(event.paid.eq(filter.paid()));
        }

        LocalDateTime startDate = filter.getRangeStartDateTime();
        LocalDateTime endDate = filter.getRangeEndDateTime();

        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                log.warn("Начало события позже завершения: start={}, end={}", startDate, endDate);
                throw new ValidationException("Начало события не может быть позже завершения события");
            }
            builder.and(event.eventDate.between(startDate, endDate));
        } else if (startDate != null) {
            builder.and(event.eventDate.after(startDate));
        } else if (endDate != null) {
            builder.and(event.eventDate.before(endDate));
        }

        if (filter.onlyAvailable()) {
            builder.and(event.confirmedRequests.lt(event.participantLimit));
        }

        log.debug("Предикат успешно сформирован");
        return builder.getValue();
    }

    private boolean sortByViews(PublicEventsFilter filter) {
        return filter.sort() != null && filter.sort().equals(EventSort.VIEWS);
    }

    private User getUserOrThrow(Long userId) {
        log.info("Получение пользователя по id={}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", userId);
                    return new NotFoundException("Пользователь с id=" + userId + " не найден");
                });
    }

    private Event getOwnedEventOrThrow(Long userId, Long eventId) {
        log.info("Получение события {} пользователя {}", eventId, userId);
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> {
                    log.warn("Событие с id={} не найдено у пользователя {}", eventId, userId);
                    return new NotFoundException("Cобытие с id=" + eventId + " не найдено");
                });
    }

    @Override
    public Map<Long, Long> getViews(List<Long> eventIds) {
        log.info("Получение статистики просмотров для событий: {}", eventIds);
        if (eventIds.isEmpty()) {
            log.debug("Список id событий пуст");
            return Map.of();
        }
        List<String> uris = eventIds.stream().map(id -> EVENT_URI_PREFIX + id).toList();

        List<ViewStats> stats = statsClient.getHits(
                STATS_RANGE_START.format(DATE_FORMATTER), LocalDateTime.now().format(DATE_FORMATTER), uris, true);
        log.debug("Статистика успешно получена");
        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.uri().substring(EVENT_URI_PREFIX.length())),
                        ViewStats::hits));
    }

    @Override
    public List<EventFullDto> getAdminEvents(AdminEventsFilter filter) {
        log.info("Поиск событий с фильтром: {}", filter);
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        if (!filter.users().isEmpty()) {
            builder.and(event.initiator.id.in(filter.users()));
        }
        if (!filter.states().isEmpty()) {
            builder.and(event.state.in(filter.states()));
        }
        if (!filter.categories().isEmpty()) {
            builder.and(event.category.id.in(filter.categories()));
        }

        LocalDateTime startDate = filter.getRangeStartDateTime();
        LocalDateTime endDate = filter.getRangeEndDateTime();
        if (startDate != null) {
            builder.and(event.eventDate.goe(startDate));
        }
        if (endDate != null) {
            builder.and(event.eventDate.loe(endDate));
        }

        List<Event> events = queryFactory
                .selectFrom(event)
                .leftJoin(event.category).fetchJoin()
                .leftJoin(event.initiator).fetchJoin()
                .where(builder.getValue())
                .offset(filter.from())
                .limit(filter.size())
                .fetch();

        Map<Long, Long> views = getViews(events.stream().map(Event::getId).toList());
        log.debug("События успешно получены для администратора");
        return events.stream()
                .map(e -> eventMapper.toFullDto(e, views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Override
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обновление события администратором: eventId={}", eventId);
        Event event = getEventByIdOrThrow(eventId);

        if (request.eventDate() != null && request.eventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            log.warn("Попытка обновить событие с датой начала раньше, чем через час");
            throw new ConflictException(
                    "Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
        }

        if (request.stateAction() != null) {
            if (request.stateAction() == AdminStateAction.PUBLISH_EVENT) {
                if (!EventState.PENDING.name().equals(event.getState())) {
                    log.warn("Попытка опубликовать событие в статусе: {}", event.getState());
                    throw new ConflictException(
                            "\n" +
                                    "\n" +
                                    "Невозможно опубликовать событие: " + event.getState());
                }
                event.setState(EventState.PUBLISHED.name());
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.stateAction() == AdminStateAction.REJECT_EVENT) {
                if (EventState.PUBLISHED.name().equals(event.getState())) {
                    log.warn("Попытка отклонить уже опубликованное событие");
                    throw new ConflictException(
                            "Невозможно отклонить событие, так как оно уже опубликовано.");
                }
                event.setState(EventState.CANCELED.name());
            }
        }

        eventMapper.updateEntityFromAdminDto(request, event);
        if (request.category() != null) {
            Category category = categoryRepository.findById(request.category())
                    .orElseThrow(() -> {
                        log.warn("Категория с id={} не найдена", request.category());
                        return new NotFoundException(
                                "Категория с id=" + request.category() + " не найдена");
                    });
            event.setCategory(category);
        }

        Event saved = eventRepository.save(event);
        Long views = getViews(List.of(saved.getId())).getOrDefault(saved.getId(), 0L);
        log.debug("Событие успешно обновлено администратором");
        return eventMapper.toFullDto(saved, views);
    }

    private Event getEventByIdOrThrow(Long eventId) {
        log.info("Получение события по id={}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие с id={} не существует", eventId);
                    return new NotFoundException("Событие с id= " + eventId + " не существует");
                });
    }

    private Double calculateDistance(Double latUser, Double lonUser, Double latEvent, Double lonEvent) {
        double radUser = Math.toRadians(latUser);
        double radEvent = Math.toRadians(latEvent);

        double radTheta = Math.toRadians(lonUser - lonEvent);

        double distance = Math.sin(radUser) * Math.sin(radEvent) + Math.cos(radUser)
                * Math.cos(radEvent) * Math.cos(radTheta);

        if (distance > 1) {
            distance = 1;
        }
        return distance = Math.acos(distance) * EARTH_RADIUS_METERS;
    }
}