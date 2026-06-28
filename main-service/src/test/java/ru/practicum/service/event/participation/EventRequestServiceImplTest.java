package ru.practicum.service.event.participation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.dto.participation.RequestStatusAction;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.request.RequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.Request;
import ru.practicum.model.User;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.repository.event.participation.EventRequestRepository;
import ru.practicum.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRequestServiceImplTest {

    @Mock
    private EventRequestRepository eventRequestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    private EventRequestServiceImpl eventRequestService;

    private User user;
    private Event event;
    private Request request;
    private ParticipationRequestDto requestDto;
    private EventRequestStatusUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Иван Петров")
                .email("ivan@example.com")
                .build();

        event = Event.builder()
                .id(1L)
                .title("Рок-концерт")
                .participantLimit(10)
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

        updateRequest = new EventRequestStatusUpdateRequest(
                List.of(1L),
                RequestStatusAction.CONFIRMED
        );
    }

    @Test
    @DisplayName("Получение запросов на участие - успешный сценарий")
    void getEventRequests_ValidData_ReturnRequestsList() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRequestRepository.findAllByEventId(1L)).thenReturn(List.of(request));
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        List<ParticipationRequestDto> result = eventRequestService.getEventRequests(1L, 1L);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(userRepository).existsById(1L);
        verify(eventRepository).findByIdAndInitiatorId(1L, 1L);
        verify(eventRequestRepository).findAllByEventId(1L);
    }

    @Test
    @DisplayName("Получение запросов на участие - пользователь не найден")
    void getEventRequests_UserNotFound_ThrowNotFoundException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventRequestService.getEventRequests(99L, 1L));

        assertEquals("Пользователя с id=99 не найдено", exception.getMessage());
        verify(userRepository).existsById(99L);
        verify(eventRepository, never()).findByIdAndInitiatorId(any(), any());
    }

    @Test
    @DisplayName("Получение запросов на участие - событие не найдено")
    void getEventRequests_EventNotFound_ThrowNotFoundException() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(99L, 1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventRequestService.getEventRequests(1L, 99L));

        assertEquals("Событие id=99 не найдено", exception.getMessage());
        verify(userRepository).existsById(1L);
        verify(eventRepository).findByIdAndInitiatorId(99L, 1L);
    }

    @Test
    @DisplayName("Обновление статусов запросов - успешный сценарий подтверждения")
    void updateRequestStatuses_ConfirmRequests_ReturnResult() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRequestRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(request));
        when(eventRequestRepository.saveAll(anyList())).thenReturn(List.of(request));
        when(eventRepository.save(event)).thenReturn(event);
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        EventRequestStatusUpdateResult result = eventRequestService.updateRequestStatuses(1L, 1L, updateRequest);

        assertNotNull(result);
        assertEquals(1, result.confirmedRequests().size());

        verify(userRepository).existsById(1L);
        verify(eventRepository).findByIdAndInitiatorId(1L, 1L);
        verify(eventRequestRepository).findAllByIdIn(List.of(1L));
        verify(eventRequestRepository).saveAll(anyList());
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("Обновление статусов запросов - успешный сценарий отклонения")
    void updateRequestStatuses_RejectRequests_ReturnResult() {
        EventRequestStatusUpdateRequest rejectRequest =
                new EventRequestStatusUpdateRequest(List.of(1L), RequestStatusAction.REJECTED);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRequestRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(request));
        when(eventRequestRepository.saveAll(anyList())).thenReturn(List.of(request));
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        EventRequestStatusUpdateResult result = eventRequestService.updateRequestStatuses(1L, 1L, rejectRequest);

        assertNotNull(result);
        assertEquals(1, result.rejectedRequests().size());

        verify(userRepository).existsById(1L);
        verify(eventRepository).findByIdAndInitiatorId(1L, 1L);
        verify(eventRequestRepository).findAllByIdIn(List.of(1L));
        verify(eventRequestRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Обновление статусов запросов - запросов не найдено")
    void updateRequestStatuses_RequestsNotFound_ThrowNotFoundException() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRequestRepository.findAllByIdIn(List.of(99L))).thenReturn(List.of());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventRequestService.updateRequestStatuses(1L, 1L,
                        new EventRequestStatusUpdateRequest(List.of(99L), RequestStatusAction.CONFIRMED)));

        assertEquals("Некоторые запросы не найдены.", exception.getMessage());
        verify(eventRequestRepository).findAllByIdIn(List.of(99L));
        verify(eventRequestRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Обновление статусов запросов - пустой запрос")
    void updateRequestStatuses_NullRequest_ThrowConflictException() {
        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventRequestService.updateRequestStatuses(1L, 1L, null));

        assertEquals("Переданные параметры не должны быть пустыми", exception.getMessage());
        verifyNoInteractions(eventRequestRepository);
    }

    @Test
    @DisplayName("Обновление статусов запросов - запрос не относится к событию")
    void updateRequestStatuses_RequestNotBelongToEvent_ThrowConflictException() {
        Event otherEvent = Event.builder().id(2L).build();
        Request otherRequest = Request.builder()
                .id(2L)
                .event(otherEvent)
                .status("PENDING")
                .build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRequestRepository.findAllByIdIn(List.of(2L))).thenReturn(List.of(otherRequest));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventRequestService.updateRequestStatuses(1L, 1L,
                        new EventRequestStatusUpdateRequest(List.of(2L), RequestStatusAction.CONFIRMED)));

        assertEquals("Запрос не относится к указанному событию.", exception.getMessage());
        verify(eventRequestRepository).findAllByIdIn(List.of(2L));
    }

    @Test
    @DisplayName("Обновление статусов запросов - запрос не в статусе PENDING")
    void updateRequestStatuses_RequestNotPending_ThrowConflictException() {
        Request confirmedRequest = Request.builder()
                .id(1L)
                .event(event)
                .status("CONFIRMED")
                .build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRequestRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(confirmedRequest));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventRequestService.updateRequestStatuses(1L, 1L, updateRequest));

        assertEquals("Запрос должен иметь статус PENDING.", exception.getMessage());
        verify(eventRequestRepository).findAllByIdIn(List.of(1L));
    }

    @Test
    @DisplayName("Обновление статусов запросов - превышен лимит участников")
    void updateRequestStatuses_ParticipantLimitExceeded_ThrowConflictException() {
        event.setParticipantLimit(1);
        event.setConfirmedRequests(1L);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRequestRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(request));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventRequestService.updateRequestStatuses(1L, 1L, updateRequest));

        assertEquals("Внимание: лимит участников!", exception.getMessage());
        verify(eventRequestRepository).findAllByIdIn(List.of(1L));
        verify(eventRequestRepository, never()).saveAll(any());
    }
}