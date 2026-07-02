package ru.practicum.controller.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.EndpointHitInfo;
import ru.practicum.StatsClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.event.param_objects.PublicEventsFilter;
import ru.practicum.dto.location.Location;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicEventController.class)
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private StatsClient statsClient;

    private EventFullDto eventFullDto;
    private EventShortDto eventShortDto;

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
                .publishedOn(LocalDateTime.now())
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Концерт в Москве")
                .views(0L)
                .build();

        eventShortDto = EventShortDto.builder()
                .annotation("Отличный концерт в Москве")
                .category(category)
                .confirmedRequests(0L)
                .eventDate(LocalDateTime.now().plusDays(7))
                .id(1L)
                .initiator(initiator)
                .paid(false)
                .title("Концерт в Москве")
                .views(0L)
                .build();
    }

    @Test
    @DisplayName("Получение опубликованных событий с фильтром - успешный сценарий")
    void getEventsByFilter_ValidFilter_ReturnListOfEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);
        when(eventService.getPublishedEvents(any(PublicEventsFilter.class), any(EndpointHitInfo.class)))
                .thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("text", "концерт")
                        .param("categories", "1")
                        .param("paid", "false")
                        .param("onlyAvailable", "false")
                        .param("sort", "EVENT_DATE")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение опубликованных событий - параметры по умолчанию")
    void getEventsByFilter_DefaultParams_ReturnListOfEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);
        when(eventService.getPublishedEvents(any(PublicEventsFilter.class), any(EndpointHitInfo.class)))
                .thenReturn(events);

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение опубликованных событий - неверная дата")
    void getEventsByFilter_InvalidDateRange_ReturnBadRequest() throws Exception {
        when(eventService.getPublishedEvents(any(PublicEventsFilter.class), any(EndpointHitInfo.class)))
                .thenThrow(new ValidationException("Начало события не может быть позже завершения события"));

        mockMvc.perform(get("/events")
                        .param("rangeStart", "2024-12-31 23:59:59")
                        .param("rangeEnd", "2024-01-01 00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение события по ID - успешный сценарий")
    void getEventById_ValidId_ReturnEvent() throws Exception {
        when(eventService.getEventById(anyLong())).thenReturn(eventFullDto);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Концерт в Москве"));
    }

    @Test
    @DisplayName("Получение события по ID - событие не найдено")
    void getEventById_EventNotFound_ReturnNotFound() throws Exception {
        when(eventService.getEventById(anyLong()))
                .thenThrow(new NotFoundException("Событие с id=999 не найдено"));

        mockMvc.perform(get("/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение события по ID - событие не опубликовано")
    void getEventById_EventNotPublished_ReturnNotFound() throws Exception {
        when(eventService.getEventById(anyLong()))
                .thenThrow(new NotFoundException("Событие не опубликовано"));

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение события по ID - отрицательный ID")
    void getEventById_NegativeId_ReturnBadRequest() throws Exception {
        mockMvc.perform(get("/events/-1"))
                .andExpect(status().isBadRequest());
    }
}