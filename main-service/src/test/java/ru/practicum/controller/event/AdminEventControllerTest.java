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
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.dto.event.param_objects.AdminEventsFilter;
import ru.practicum.dto.location.Location;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEventController.class)
class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private StatsClient statsClient;

    @Autowired
    private ObjectMapper objectMapper;

    private EventFullDto eventFullDto;
    private UpdateEventAdminRequest updateRequest;

    @BeforeEach
    void setUp() {
        CategoryDto category = new CategoryDto(1L, "Концерты");
        UserShortDto initiator = new UserShortDto(1L, "Иван Иванов");
        Location location = new Location(55.7558f, 37.6173f);

        eventFullDto = new EventFullDto(
                "Отличный концерт в Москве",
                category,
                0L,
                LocalDateTime.now().minusDays(1),
                "Подробное описание концерта",
                LocalDateTime.now().plusDays(7),
                1L,
                initiator,
                location,
                false,
                100,
                null,
                true,
                EventState.PENDING,
                "Концерт в Москве",
                0L
        );

        updateRequest = new UpdateEventAdminRequest(
                "Обновленная аннотация концерта",
                1L,
                "Обновленное описание концерта",
                LocalDateTime.now().plusDays(10),
                location,
                true,
                200,
                false,
                null,
                "Обновленный концерт"
        );
    }

    @Test
    @DisplayName("Получение событий администратором - успешный сценарий")
    void getAdminEvents_ValidFilter_ReturnListOfEvents() throws Exception {
        List<EventFullDto> events = List.of(eventFullDto);
        when(eventService.getAdminEvents(any(AdminEventsFilter.class))).thenReturn(events);

        mockMvc.perform(get("/admin/events")
                        .param("users", "1")
                        .param("states", "PENDING")
                        .param("categories", "1")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение событий администратором - пустой список")
    void getAdminEvents_EmptyList_ReturnEmptyList() throws Exception {
        when(eventService.getAdminEvents(any(AdminEventsFilter.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Обновление события администратором - успешный сценарий")
    void updateAdminEvent_ValidData_ReturnUpdatedEvent() throws Exception {
        EventFullDto updatedEvent = new EventFullDto(
                "Обновленная аннотация концерта",
                new CategoryDto(1L, "Концерты"),
                0L,
                LocalDateTime.now().minusDays(1),
                "Обновленное описание концерта",
                LocalDateTime.now().plusDays(10),
                1L,
                new UserShortDto(1L, "Иван Иванов"),
                new Location(55.7558f, 37.6173f),
                true,
                200,
                LocalDateTime.now(),
                false,
                EventState.PUBLISHED,
                "Обновленный концерт",
                0L
        );

        when(eventService.updateAdminEvent(anyLong(), any(UpdateEventAdminRequest.class)))
                .thenReturn(updatedEvent);

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.annotation").value("Обновленная аннотация концерта"));
    }

    @Test
    @DisplayName("Обновление события администратором - событие не найдено")
    void updateAdminEvent_EventNotFound_ReturnNotFound() throws Exception {
        when(eventService.updateAdminEvent(anyLong(), any(UpdateEventAdminRequest.class)))
                .thenThrow(new NotFoundException("Событие с id=999 не найдено"));

        mockMvc.perform(patch("/admin/events/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление события администратором - конфликт статуса")
    void updateAdminEvent_ConflictStatus_ReturnConflict() throws Exception {
        when(eventService.updateAdminEvent(anyLong(), any(UpdateEventAdminRequest.class)))
                .thenThrow(new ConflictException("Нельзя опубликовать событие в статусе PUBLISHED"));

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());
    }
}