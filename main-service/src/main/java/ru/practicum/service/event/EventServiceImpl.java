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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.ViewStats;
import ru.practicum.dto.event.*;
import ru.practicum.dto.event.param_objects.PublicEventsFilter;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.event.EventMapper;
import ru.practicum.model.*;
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
        queryFactory = new JPAQueryFactory(entityManager);
    }

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
        Map<Long, Long> viewsMap = getViews(events.stream().map(Event::getId).toList());
        return events.stream()
                .map(event -> eventMapper.toShortDto(event, viewsMap))
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
        log.info("Получение подробного описания ивента по его id= {}", eventId);
        getUserOrThrow(userId);
        Event event = getOwnedEventOrThrow(userId, eventId);
        log.debug("Получение Views для ивента");
        Long views = getViews(List.of(event.getId())).getOrDefault(event.getId(), 0L);
        log.debug("Views успешно получены");
        log.info("Описание ивента успешно получено");
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

    @Override
    public List<EventShortDto> getPublishedEvents(PublicEventsFilter filter) {
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
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(event.state.eq(String.valueOf(EventState.PENDING)));

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
            builder.and(event.eventDate.between(startDate, endDate));
        } else if (startDate != null) {
            builder.and(event.eventDate.after(startDate));
        } else if (endDate != null) {
            builder.and(event.eventDate.before(endDate));
        }

        if (filter.onlyAvailable()) {
            builder.and(event.confirmedRequests.lt(event.participantLimit));
        }

        return builder.getValue();
    }

    private boolean sortByViews(PublicEventsFilter filter) {
        return filter.sort() != null && filter.sort().equals(EventSort.VIEWS);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event getOwnedEventOrThrow(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Cобытие с id=" + eventId + " не найдено"));
    }

    @Override
    public Map<Long, Long> getViews(List<Long> eventIds) {
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
