package ru.practicum.service.event;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.StatsClient;
import ru.practicum.dto.event.*;
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

        EventFullDto result = eventService.getEventById(1L, null, null);

        assertNotNull(result);
        assertEquals(eventFullDto.id(), result.id());
        assertEquals(eventFullDto.title(), result.title());

        verify(eventRepository).findById(1L);
        verify(eventMapper).toFullDto(eq(event), anyLong());
    }

    @Test
    @DisplayName("Получение события по ID - событие не опубликовано")
    void getEventById_EventNotPublished_ThrowNotFoundException() {
        event.setState("PENDING");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventById(1L, null, null));

        assertEquals("Ивент не опубликован", exception.getMessage());
        verify(eventRepository).findById(1L);
    }

    @Test
    @DisplayName("Получение события по ID - событие не найдено")
    void getEventById_EventNotFound_ThrowNotFoundException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventById(1L, null, null));

        assertEquals("Событие с id= 99 не существует", exception.getMessage());
        verify(eventRepository).findById(99L);
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