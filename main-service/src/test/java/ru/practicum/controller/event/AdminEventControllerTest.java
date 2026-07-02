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

        eventFullDto = EventFullDto.builder()
                .annotation("Отличный концерт в Москве")
                .category(category)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now().minusDays(1))
                .description("Подробное описание концерта")
                .eventDate(LocalDateTime.now().plusDays(7))
                .id(1L)
                .initiator(initiator)
                .location(location)
                .paid(false)
                .participantLimit(100)
                .publishedOn(null)
                .requestModeration(true)
                .state(EventState.PENDING)
                .title("Концерт в Москве")
                .views(0L)
                .build();

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

        EventFullDto updatedEvent = EventFullDto.builder()
                .annotation("Обновленная аннотация концерта")
                .category(new CategoryDto(1L, "Концерты"))
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now().minusDays(1))
                .description("Обновленное описание концерта")
                .eventDate(LocalDateTime.now().plusDays(10))
                .id(1L)
                .initiator(new UserShortDto(1L, "Иван Иванов"))
                .location(new Location(55.7558f, 37.6173f))
                .paid(true)
                .participantLimit(200)
                .publishedOn(LocalDateTime.now())
                .requestModeration(false)
                .state(EventState.PUBLISHED)
                .title("Обновленный концерт")
                .views(0L)
                .build();

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