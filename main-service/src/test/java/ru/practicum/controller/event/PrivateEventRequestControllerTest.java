package ru.practicum.controller.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.StatsClient;
import ru.practicum.dto.participation.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.service.event.participation.EventRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateEventRequestController.class)
class PrivateEventRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventRequestService requestService;

    @MockBean
    private StatsClient statsClient;

    @Autowired
    private ObjectMapper objectMapper;

    private ParticipationRequestDto requestDto;
    private EventRequestStatusUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        requestDto = new ParticipationRequestDto(
                LocalDateTime.now(),
                1L,
                1L,
                2L,
                ParticipationStatus.PENDING
        );

        updateRequest = new EventRequestStatusUpdateRequest(
                List.of(1L, 2L),
                RequestStatusAction.CONFIRMED
        );
    }

    @Test
    @DisplayName("Получение запросов на участие в событии - успешный сценарий")
    void getEventRequests_ValidIds_ReturnListOfRequests() throws Exception {
        List<ParticipationRequestDto> requests = List.of(requestDto);
        when(requestService.getEventRequests(anyLong(), anyLong())).thenReturn(requests);

        mockMvc.perform(get("/users/1/events/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение запросов на участие в событии - событие не найдено")
    void getEventRequests_EventNotFound_ReturnNotFound() throws Exception {
        when(requestService.getEventRequests(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Событие с id=999 не найдено"));

        mockMvc.perform(get("/users/1/events/999/requests"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление статуса запросов - успешный сценарий")
    void updateRequestStatuses_ValidData_ReturnUpdateResult() throws Exception {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult(
                List.of(requestDto),
                List.of()
        );

        when(requestService.updateRequestStatuses(anyLong(), anyLong(), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(result);

        mockMvc.perform(patch("/users/1/events/1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests.length()").value(1))
                .andExpect(jsonPath("$.confirmedRequests[0].id").value(1L));
    }

    @Test
    @DisplayName("Обновление статуса запросов - пустой список запросов")
    void updateRequestStatuses_EmptyRequests_ReturnBadRequest() throws Exception {
        EventRequestStatusUpdateRequest invalidRequest = new EventRequestStatusUpdateRequest(
                List.of(),
                RequestStatusAction.CONFIRMED
        );

        mockMvc.perform(patch("/users/1/events/1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Обновление статуса запросов - превышен лимит участников")
    void updateRequestStatuses_LimitExceeded_ReturnConflict() throws Exception {
        when(requestService.updateRequestStatuses(anyLong(), anyLong(), any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new ConflictException("Достигнут лимит участников события"));

        mockMvc.perform(patch("/users/1/events/1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());
    }
}