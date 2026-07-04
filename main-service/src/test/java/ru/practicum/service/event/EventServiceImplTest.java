package ru.practicum.service.event;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.StatsClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.event.param_objects.PrivateEventsFilter;
import ru.practicum.dto.event.projection.EventShortProjection;
import ru.practicum.dto.user.UserShortDto;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private StatsClient statsClient;

    @Mock
    private JPAQueryFactory queryFactory;

    @InjectMocks
    private EventServiceImpl eventService;

    private User user;
    private Category category;
    private Event event;
    private EventFullDto eventFullDto;
    private NewEventDto newEventDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Иван Петров")
                .email("ivan@example.com")
                .build();

        category = Category.builder()
                .id(1L)
                .name("Концерты")
                .build();

        event = Event.builder()
                .id(1L)
                .title("Рок-концерт")
                .annotation("Грандиозный рок-концерт")
                .description("Описание рок-концерта")
                .category(category)
                .initiator(user)
                .eventDate(LocalDateTime.now().plusDays(10))
                .createdOn(LocalDateTime.now())
                .state("PUBLISHED")
                .confirmedRequests(0L)
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .build();

        eventFullDto = EventFullDto.builder()
                .annotation("Грандиозный рок-концерт")
                .category(null)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now())
                .description("Описание рок-концерта")
                .eventDate(LocalDateTime.now().plusDays(10))
                .id(1L)
                .initiator(null)
                .location(null)
                .paid(true)
                .participantLimit(100)
                .publishedOn(null)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Рок-концерт")
                .views(0L)
                .distance(null)
                .build();

        newEventDto = new NewEventDto(
                "Грандиозный рок-концерт",
                1L,
                "Описание рок-концерта",
                LocalDateTime.now().plusDays(10),
                null,
                true,
                100,
                true,
                "Рок-концерт"
        );
    }

    @Test
    @DisplayName("Получение события по ID - успешный сценарий")
    void getEventById_ValidId_ReturnEventFullDto() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toFullDto(eq(event), anyLong())).thenReturn(eventFullDto);

        EventFullDto result = eventService.getEventById(1L);

        assertNotNull(result);
        assertEquals(eventFullDto.id(), result.id());
        assertEquals(eventFullDto.title(), result.title());
        assertNull(result.distance());

        verify(eventRepository).findById(1L);
        verify(eventMapper).toFullDto(eq(event), anyLong());
    }

    @Test
    @DisplayName("Получение события по ID - событие не опубликовано")
    void getEventById_EventNotPublished_ThrowNotFoundException() {
        event.setState("PENDING");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventById(1L));

        assertEquals("Ивент не опубликован", exception.getMessage());
        verify(eventRepository).findById(1L);
    }

    @Test
    @DisplayName("Получение события по ID - событие не найдено")
    void getEventById_EventNotFound_ThrowNotFoundException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventById(99L));

        assertEquals("Событие с id= 99 не существует", exception.getMessage());
        verify(eventRepository).findById(99L);
    }

    @Test
    @DisplayName("Получение события по ID c дистанцией - успешный сценарий")
    void getEventWithDistance_withValidCoordinates_ReturnEventFullDto() {
        Long eventId = 1L;
        Double lat = 55.7558;
        Double lon = 37.6173;

        Event event = new Event();
        event.setId(eventId);
        event.setLat(55.7512);
        event.setLon(37.6185);
        event.setState(String.valueOf(EventState.PUBLISHED));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventMapper.toFullDto(any(), anyLong())).thenReturn(
                EventFullDto.builder().id(eventId).build()
        );

        EventFullDto result = eventService.getEventWithDistance(eventId, lat, lon);

        assertNotNull(result);
        assertEquals(eventId, result.id());

        assertTrue(result.distance() > 500 && result.distance() < 540);

        verify(eventRepository).findById(1L);
        verify(eventMapper).toFullDto(any(), anyLong());
    }

    @Test
    @DisplayName("Получение события по ID c дистанцией - событие не найдено")
    void getEventWithDistance_EventNotFound_ThrowNotFoundException() {
        Long eventId = 99L;
        Double lat = 55.7558;
        Double lon = 37.6173;

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventWithDistance(eventId, lat, lon));

        assertEquals("Событие с id= 99 не существует", exception.getMessage());
        verify(eventRepository).findById(99L);
        verify(eventMapper, never()).toFullDto(any(), anyLong());
    }

    @Test
    @DisplayName("Получение события по ID c дистанцией - событие не опубликовано")
    void getEventWithDistance_EventNotPublished_ThrowNotFoundException() {
        Long eventId = 1L;
        Double lat = 55.7558;
        Double lon = 37.6173;

        Event event = new Event();
        event.setId(eventId);
        event.setLat(55.7512);
        event.setLon(37.6185);
        event.setState(String.valueOf(EventState.PENDING));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventWithDistance(eventId, lat, lon));

        assertEquals("Ивент не опубликован", exception.getMessage());
        verify(eventRepository).findById(1L);
        verify(eventMapper, never()).toFullDto(any(), anyLong());
    }

    @Test
    @DisplayName("Создание события - успешный сценарий")
    void createEvent_ValidData_ReturnCreatedEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventMapper.toEntity(newEventDto)).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toFullDto(eq(event), anyLong())).thenReturn(eventFullDto);

        EventFullDto result = eventService.createEvent(1L, newEventDto);

        assertNotNull(result);
        assertEquals(eventFullDto.id(), result.id());
        assertNull(result.distance());

        verify(userRepository).findById(1L);
        verify(categoryRepository).findById(1L);
        verify(eventMapper).toEntity(newEventDto);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("Создание события - пользователь не найден")
    void createEvent_UserNotFound_ThrowNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.createEvent(99L, newEventDto));

        assertEquals("Пользователь с id=99 не найден", exception.getMessage());
        verify(userRepository).findById(99L);
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Создание события - категория не найдена")
    void createEvent_CategoryNotFound_ThrowNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        NewEventDto invalidDto = new NewEventDto(
                "Грандиозный рок-концерт",
                99L,
                "Описание",
                LocalDateTime.now().plusDays(10),
                null,
                true,
                100,
                true,
                "Рок-концерт"
        );

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.createEvent(1L, invalidDto));

        assertEquals("Категория с id=99 не найдена", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(categoryRepository).findById(99L);
    }

    @Test
    @DisplayName("Создание события - дата начала раньше чем через 2 часа")
    void createEvent_EventDateTooSoon_ThrowValidationException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        NewEventDto invalidDto = new NewEventDto(
                "Грандиозный рок-концерт",
                1L,
                "Описание",
                LocalDateTime.now().plusMinutes(30),
                null,
                true,
                100,
                true,
                "Рок-концерт"
        );

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.createEvent(1L, invalidDto));

        assertEquals("Дата события не может быть раньше, чем через два часа от текущего момента", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(categoryRepository).findById(1L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение подборки событий по координатам - успешный сценарий")
    void getUserEventsByCoordinates_ValidCoordinates_ReturnCollectionEventShortDto() {
        Long userId = 1L;
        Double radiusMeters = 1000.0;
        Double lat = 55.7558;
        Double lon = 37.6173;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        EventShortProjection eventProjection = mock(EventShortProjection.class);
        when(eventProjection.getId()).thenReturn(1L);
        when(eventProjection.getAnnotation()).thenReturn("Отличный концерт в Москве");
        when(eventProjection.getCategory()).thenReturn(category);
        when(eventProjection.getConfirmedRequests()).thenReturn(0L);
        when(eventProjection.getEventDate()).thenReturn(LocalDateTime.now().plusDays(7));
        when(eventProjection.getInitiator()).thenReturn(user);
        when(eventProjection.getPaid()).thenReturn(false);
        when(eventProjection.getTitle()).thenReturn("Концерт в Москве");
        when(eventProjection.getDistance()).thenReturn(300.0);

        EventShortDto expectedDto = EventShortDto.builder()
                .id(1L)
                .annotation("Отличный концерт в Москве")
                .category(new CategoryDto(1L, "Концерты"))
                .confirmedRequests(0L)
                .eventDate(LocalDateTime.now().plusDays(7))
                .initiator(new UserShortDto(1L, "Иван Иванов"))
                .paid(false)
                .title("Концерт в Москве")
                .distance(300.0)
                .views(0L)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findEventsWithinRadius(radiusMeters, lat, lon, pageable))
                .thenReturn(List.of(eventProjection));
        when(statsClient.getHits(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        List<EventShortDto> result = eventService.getUserEventsByCoordinates(
                userId,
                new PrivateEventsFilter(radiusMeters, lat, lon),
                page,
                size
        );

        assertNotNull(result);
        assertEquals(1, result.size());

        EventShortDto actualDto = result.get(0);
        assertEquals(expectedDto.id(), actualDto.id());
        assertEquals(expectedDto.annotation(), actualDto.annotation());
        assertEquals(expectedDto.title(), actualDto.title());
        assertEquals(expectedDto.distance(), actualDto.distance());

        verify(userRepository).findById(userId);
        verify(eventRepository).findEventsWithinRadius(radiusMeters, lat, lon, pageable);
        verify(statsClient).getHits(anyString(), anyString(), anyList(), anyBoolean());
        verifyNoMoreInteractions(userRepository, eventRepository, statsClient);
    }

    @Test
    @DisplayName("Получение подборки событий по координатам - пустой список")
    void getUserEventsByCoordinates_NoEventsInRadius_ReturnEmptyList() {
        Long userId = 1L;
        Double radiusMeters = 1000.0;
        Double lat = 55.7558;
        Double lon = 37.6173;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findEventsWithinRadius(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(List.of());

        List<EventShortDto> result = eventService.getUserEventsByCoordinates(userId,
                new PrivateEventsFilter(radiusMeters, lat, lon), page, size);

        assertTrue(result.isEmpty());
        verify(userRepository).findById(1L);
        verify(eventRepository).findEventsWithinRadius(radiusMeters, lat, lon, pageable);
        verify(statsClient, never()).getHits(anyString(), anyString(), anyList(), anyBoolean());
    }

    @Test
    @DisplayName("Получение подборки событий по координатам - пользователь не найдено")
    void getUserEventsByCoordinates_EventNotFound_ThrowNotFoundException() {
        Long userId = 99L;
        Double radiusMeters = 1000.0;
        Double lat = 55.7558;
        Double lon = 37.6173;
        int page = 0;
        int size = 10;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getUserEventsByCoordinates(userId,
                        new PrivateEventsFilter(radiusMeters, lat, lon), page, size));

        assertEquals("Пользователь с id=99 не найден", exception.getMessage());
        verify(userRepository).findById(99L);
        verify(eventRepository, never()).findEventsWithinRadius(anyDouble(), anyDouble(), anyDouble(), any());
        verify(statsClient, never()).getHits(anyString(), anyString(), anyList(), anyBoolean());
    }

    @Test
    @DisplayName("Обновление события администратором - успешный сценарий публикации")
    void updateAdminEvent_PublishEvent_ReturnUpdatedEvent() {
        event.setState("PENDING");
        UpdateEventAdminRequest request = new UpdateEventAdminRequest(
                null, null, null, null, null, null, null, null,
                AdminStateAction.PUBLISH_EVENT, null
        );

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toFullDto(eq(event), anyLong())).thenReturn(eventFullDto);

        EventFullDto result = eventService.updateAdminEvent(1L, request);

        assertNotNull(result);
        assertEquals("PUBLISHED", event.getState());
        assertNotNull(event.getPublishedOn());
        assertNull(result.distance());

        verify(eventRepository).findById(1L);
        verify(eventRepository).save(event);
        verify(eventMapper).toFullDto(eq(event), anyLong());
    }

    @Test
    @DisplayName("Обновление события администратором - публикация события не в статусе PENDING")
    void updateAdminEvent_PublishEventNotPending_ThrowConflictException() {
        event.setState("CANCELED");
        UpdateEventAdminRequest request = new UpdateEventAdminRequest(
                null, null, null, null, null, null, null, null,
                AdminStateAction.PUBLISH_EVENT, null
        );

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateAdminEvent(1L, request));

        assertEquals("\n\nНевозможно опубликовать событие: " + event.getState(), exception.getMessage());
        verify(eventRepository).findById(1L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление события администратором - отклонение опубликованного события")
    void updateAdminEvent_RejectPublishedEvent_ThrowConflictException() {
        event.setState("PUBLISHED");
        UpdateEventAdminRequest request = new UpdateEventAdminRequest(
                null, null, null, null, null, null, null, null,
                AdminStateAction.REJECT_EVENT, null
        );

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateAdminEvent(1L, request));

        assertEquals("Невозможно отклонить событие, так как оно уже опубликовано.", exception.getMessage());
        verify(eventRepository).findById(1L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление события администратором - событие не найдено")
    void updateAdminEvent_EventNotFound_ThrowNotFoundException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateEventAdminRequest request = new UpdateEventAdminRequest(
                null, null, null, null, null, null, null, null, null, null
        );

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.updateAdminEvent(99L, request));

        assertEquals("Событие с id= 99 не существует", exception.getMessage());
        verify(eventRepository).findById(99L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление события пользователем - успешный сценарий")
    void updateUserEvent_ValidData_ReturnUpdatedEvent() {
        event.setState("PENDING");
        UpdateEventUserRequest request = new UpdateEventUserRequest(
                "Обновленная аннотация", null, null, null, null, null, null, null, null, null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toFullDto(eq(event), anyLong())).thenReturn(eventFullDto);

        EventFullDto result = eventService.updateUserEvent(1L, 1L, request);

        assertNotNull(result);
        assertNull(result.distance());

        verify(userRepository).findById(1L);
        verify(eventRepository).findByIdAndInitiatorId(1L, 1L);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("Обновление события пользователем - событие уже опубликовано")
    void updateUserEvent_EventPublished_ThrowConflictException() {
        event.setState("PUBLISHED");
        UpdateEventUserRequest request = new UpdateEventUserRequest(
                "Обновленная аннотация", null, null, null, null, null, null, null, null, null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateUserEvent(1L, 1L, request));

        assertEquals("Изменить можно только ожидающие или отмененные события", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(eventRepository).findByIdAndInitiatorId(1L, 1L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление события пользователем - событие не найдено")
    void updateUserEvent_EventNotFound_ThrowNotFoundException() {
        UpdateEventUserRequest request = new UpdateEventUserRequest(
                "Обновленная аннотация", null, null, null, null, null, null, null, null, null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(99L, 1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.updateUserEvent(1L, 99L, request));

        assertEquals("Cобытие с id=99 не найдено", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(eventRepository).findByIdAndInitiatorId(99L, 1L);
    }
}