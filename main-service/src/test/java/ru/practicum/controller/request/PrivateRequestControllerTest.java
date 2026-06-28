package ru.practicum.controller.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.StatsClient;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.dto.participation.ParticipationStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.service.request.RequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateRequestController.class)
class PrivateRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    @MockBean
    private StatsClient statsClient;

    @Autowired
    private ObjectMapper objectMapper;

    private ParticipationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        requestDto = new ParticipationRequestDto(
                LocalDateTime.now(),
                1L,
                1L,
                2L,
                ParticipationStatus.PENDING
        );
    }

    @Test
    @DisplayName("Получение запросов пользователя - успешный сценарий")
    void getUserRequests_ValidUserId_ReturnListOfRequests() throws Exception {
        List<ParticipationRequestDto> requests = List.of(requestDto);
        when(requestService.getUserRequests(anyLong())).thenReturn(requests);

        mockMvc.perform(get("/users/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение запросов пользователя - пользователь не найден")
    void getUserRequests_UserNotFound_ReturnNotFound() throws Exception {
        when(requestService.getUserRequests(anyLong()))
                .thenThrow(new NotFoundException("Пользователь с id=999 не найден"));

        mockMvc.perform(get("/users/999/requests"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Создание запроса на участие - успешный сценарий")
    void addParticipationRequest_ValidData_ReturnCreatedRequest() throws Exception {
        when(requestService.addParticipationRequest(anyLong(), anyLong())).thenReturn(requestDto);

        mockMvc.perform(post("/users/1/requests")
                        .param("eventId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("Создание запроса на участие - повторный запрос")
    void addParticipationRequest_DuplicateRequest_ReturnConflict() throws Exception {
        when(requestService.addParticipationRequest(anyLong(), anyLong()))
                .thenThrow(new ConflictException("Нельзя добавить повторный запрос"));

        mockMvc.perform(post("/users/1/requests")
                        .param("eventId", "1"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Создание запроса на участие - событие не опубликовано")
    void addParticipationRequest_EventNotPublished_ReturnConflict() throws Exception {
        when(requestService.addParticipationRequest(anyLong(), anyLong()))
                .thenThrow(new ConflictException("Нельзя участвовать в неопубликованном событии"));

        mockMvc.perform(post("/users/1/requests")
                        .param("eventId", "1"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Создание запроса на участие - превышен лимит участников")
    void addParticipationRequest_LimitExceeded_ReturnConflict() throws Exception {
        when(requestService.addParticipationRequest(anyLong(), anyLong()))
                .thenThrow(new ConflictException("В событии больше нет свободных мест"));

        mockMvc.perform(post("/users/1/requests")
                        .param("eventId", "1"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Отмена запроса - успешный сценарий")
    void cancelRequest_ValidData_ReturnCanceledRequest() throws Exception {
        ParticipationRequestDto canceledRequest = new ParticipationRequestDto(
                LocalDateTime.now(),
                1L,
                1L,
                2L,
                ParticipationStatus.CANCELED
        );

        when(requestService.cancelRequest(anyLong(), anyLong())).thenReturn(canceledRequest);

        mockMvc.perform(patch("/users/1/requests/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("Отмена запроса - запрос не найден")
    void cancelRequest_RequestNotFound_ReturnNotFound() throws Exception {
        when(requestService.cancelRequest(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Запрос с id=999 не найден"));

        mockMvc.perform(patch("/users/1/requests/999/cancel"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Отмена запроса - запрос принадлежит другому пользователю")
    void cancelRequest_RequestBelongsToAnotherUser_ReturnNotFound() throws Exception {
        when(requestService.cancelRequest(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Запрос принадлежит другому пользователю"));

        mockMvc.perform(patch("/users/1/requests/999/cancel"))
                .andExpect(status().isNotFound());
    }
}