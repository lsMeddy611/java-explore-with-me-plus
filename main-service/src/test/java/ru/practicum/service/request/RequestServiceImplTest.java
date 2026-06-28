package ru.practicum.service.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.participation.ParticipationRequestDto;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestMapper requestMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private RequestServiceImpl requestService;

    private User user;
    private User eventOwner;
    private Event event;
    private Request request;
    private ParticipationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Иван Петров")
                .email("ivan@example.com")
                .build();

        eventOwner = User.builder()
                .id(2L)
                .name("Петр Иванов")
                .email("petr@example.com")
                .build();

        event = Event.builder()
                .id(1L)
                .title("Рок-концерт")
                .initiator(eventOwner)
                .participantLimit(10)
                .publishedOn(LocalDateTime.now())
                .confirmedRequests(0L)
                .build();

        request = Request.builder()
                .id(1L)
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .status("PENDING")
                .build();

        requestDto = new ParticipationRequestDto(
                LocalDateTime.now(),
                1L,
                1L,
                1L,
                ru.practicum.dto.participation.ParticipationStatus.PENDING
        );
    }

    @Test
    @DisplayName("Получение запросов пользователя - успешный сценарий")
    void getUserRequests_ValidUserId_ReturnRequestsList() {
        when(requestRepository.findAllByRequesterId(1L)).thenReturn(List.of(request));
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        List<ParticipationRequestDto> result = requestService.getUserRequests(1L);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(requestRepository).findAllByRequesterId(1L);
        verify(requestMapper).toDto(request);
    }

    @Test
    @DisplayName("Получение запросов пользователя - пустой список")
    void getUserRequests_NoRequests_ReturnEmptyList() {
        when(requestRepository.findAllByRequesterId(1L)).thenReturn(List.of());

        List<ParticipationRequestDto> result = requestService.getUserRequests(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(requestRepository).findAllByRequesterId(1L);
        verify(requestMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Добавление запроса на участие - успешный сценарий")
    void addParticipationRequest_ValidData_ReturnCreatedRequest() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestService.addParticipationRequest(1L, 1L);

        assertNotNull(result);
        assertEquals(requestDto.id(), result.id());

        verify(requestRepository).existsByRequesterIdAndEventId(1L, 1L);
        verify(userRepository).findById(1L);
        verify(eventRepository).findById(1L);
        verify(requestRepository).save(any(Request.class));
        verify(requestMapper).toDto(request);
    }

    @Test
    @DisplayName("Добавление запроса на участие - повторный запрос")
    void addParticipationRequest_DuplicateRequest_ThrowConflictException() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.addParticipationRequest(1L, 1L));

        assertEquals("Нельзя добавить повторный запрос", exception.getMessage());
        verify(requestRepository).existsByRequesterIdAndEventId(1L, 1L);
        verify(userRepository, never()).findById(any());
        verify(eventRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Добавление запроса на участие - пользователь не найден")
    void addParticipationRequest_UserNotFound_ThrowNotFoundException() {
        when(requestRepository.existsByRequesterIdAndEventId(99L, 1L)).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.addParticipationRequest(99L, 1L));

        assertEquals("Пользователь с id=99 не найден", exception.getMessage());
        verify(requestRepository).existsByRequesterIdAndEventId(99L, 1L);
        verify(userRepository).findById(99L);
        verify(eventRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Добавление запроса на участие - событие не найдено")
    void addParticipationRequest_EventNotFound_ThrowNotFoundException() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 99L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.addParticipationRequest(1L, 99L));

        assertEquals("Событие с id=99 не найдено", exception.getMessage());
        verify(requestRepository).existsByRequesterIdAndEventId(1L, 99L);
        verify(userRepository).findById(1L);
        verify(eventRepository).findById(99L);
    }

    @Test
    @DisplayName("Добавление запроса на участие - владелец события")
    void addParticipationRequest_RequesterIsEventOwner_ThrowConflictException() {
        event.setInitiator(user);

        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.addParticipationRequest(1L, 1L));

        assertEquals("Запрос на участие события не может быть создан его владельцем id= 1", exception.getMessage());
        verify(requestRepository).existsByRequesterIdAndEventId(1L, 1L);
        verify(userRepository).findById(1L);
        verify(eventRepository).findById(1L);
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Добавление запроса на участие - событие не опубликовано")
    void addParticipationRequest_EventNotPublished_ThrowConflictException() {
        event.setPublishedOn(null);

        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.addParticipationRequest(1L, 1L));

        assertEquals("Нельзя участвовать в неопубликованном событии id: 1", exception.getMessage());
        verify(requestRepository).existsByRequesterIdAndEventId(1L, 1L);
        verify(userRepository).findById(1L);
        verify(eventRepository).findById(1L);
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Добавление запроса на участие - превышен лимит участников")
    void addParticipationRequest_ParticipantLimitExceeded_ThrowConflictException() {
        event.setParticipantLimit(1);
        when(requestRepository.countByEventId(1L)).thenReturn(1);

        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.addParticipationRequest(1L, 1L));

        assertEquals("В событии id: 1 больше нет свободных мест", exception.getMessage());
        verify(requestRepository).existsByRequesterIdAndEventId(1L, 1L);
        verify(userRepository).findById(1L);
        verify(eventRepository).findById(1L);
        verify(requestRepository).countByEventId(1L);
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Добавление запроса на участие - без лимита участников (автоподтверждение)")
    void addParticipationRequest_NoParticipantLimit_AutoConfirm() {
        event.setParticipantLimit(0);

        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestService.addParticipationRequest(1L, 1L);

        assertNotNull(result);

        verify(requestRepository).save(any(Request.class));
        verify(requestMapper).toDto(request);
    }

    @Test
    @DisplayName("Отмена запроса - успешный сценарий")
    void cancelRequest_ValidData_ReturnCanceledRequest() {
        when(requestRepository.findByIdAndRequesterId(1L, 1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(request)).thenReturn(request);
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestService.cancelRequest(1L, 1L);

        assertNotNull(result);
        assertEquals(requestDto.id(), result.id());

        verify(requestRepository).findByIdAndRequesterId(1L, 1L);
        verify(requestRepository).save(request);
        verify(requestMapper).toDto(request);
    }

    @Test
    @DisplayName("Отмена запроса - запрос не найден")
    void cancelRequest_RequestNotFound_ThrowNotFoundException() {
        when(requestRepository.findByIdAndRequesterId(99L, 1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.cancelRequest(1L, 99L));

        assertEquals("Запрос с id=99 не найден или принадлежит другому пользователю", exception.getMessage());
        verify(requestRepository).findByIdAndRequesterId(99L, 1L);
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Отмена запроса - запрос принадлежит другому пользователю")
    void cancelRequest_RequestBelongsToOtherUser_ThrowNotFoundException() {
        when(requestRepository.findByIdAndRequesterId(1L, 99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.cancelRequest(99L, 1L));

        assertEquals("Запрос с id=1 не найден или принадлежит другому пользователю", exception.getMessage());
        verify(requestRepository).findByIdAndRequesterId(1L, 99L);
        verify(requestRepository, never()).save(any());
    }
}