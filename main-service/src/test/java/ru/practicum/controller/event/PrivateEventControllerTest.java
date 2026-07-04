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
import ru.practicum.dto.event.*;
import ru.practicum.dto.event.param_objects.PrivateEventsFilter;
import ru.practicum.dto.location.Location;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateEventController.class)
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private StatsClient statsClient;

    @Autowired
    private ObjectMapper objectMapper;

    private NewEventDto newEventDto;
    private EventFullDto eventFullDto;
    private EventShortDto eventShortDto;

    @BeforeEach
    void setUp() {
        Location location = new Location(55.7558f, 37.6173f);

        newEventDto = new NewEventDto(
                "Отличный концерт в Москве",
                1L,
                "Подробное описание концерта с участием известных артистов",
                LocalDateTime.now().plusDays(7),
                location,
                false,
                100,
                true,
                "Концерт в Москве"
        );

        CategoryDto category = new CategoryDto(1L, "Концерты");
        UserShortDto initiator = new UserShortDto(1L, "Иван Иванов");

        eventFullDto = EventFullDto.builder()
                .annotation("Отличный концерт в Москве")
                .category(category)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now())
                .description("Подробное описание концерта...")
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
    @DisplayName("Получение событий пользователя - успешный сценарий")
    void getUserEvents_ValidUserId_ReturnListOfEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);
        when(eventService.getUserEvents(anyLong(), anyInt(), anyInt())).thenReturn(events);

        mockMvc.perform(get("/users/1/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение событий пользователя - пользователь не найден")
    void getUserEvents_UserNotFound_ReturnNotFound() throws Exception {
        when(eventService.getUserEvents(anyLong(), anyInt(), anyInt()))
                .thenThrow(new NotFoundException("Пользователь с id=999 не найден"));

        mockMvc.perform(get("/users/999/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Создание события - успешный сценарий")
    void createEvent_ValidData_ReturnCreatedEvent() throws Exception {
        when(eventService.createEvent(anyLong(), any(NewEventDto.class))).thenReturn(eventFullDto);

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Концерт в Москве"));
    }

    @Test
    @DisplayName("Создание события - дата начала раньше, чем через 2 часа")
    void createEvent_EventDateTooSoon_ReturnBadRequest() throws Exception {
        NewEventDto invalidDto = new NewEventDto(
                "Отличный концерт в Москве",
                1L,
                "Подробное описание концерта",
                LocalDateTime.now().plusHours(1),
                new Location(55.7558f, 37.6173f),
                false,
                100,
                true,
                "Концерт в Москве"
        );

        when(eventService.createEvent(anyLong(), any(NewEventDto.class)))
                .thenThrow(new ValidationException("Дата события не может быть раньше, чем через два часа"));

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание события - аннотация слишком короткая")
    void createEvent_AnnotationTooShort_ReturnBadRequest() throws Exception {
        NewEventDto invalidDto = new NewEventDto(
                "Короткая анн",
                1L,
                "Подробное описание концерта с участием известных артистов",
                LocalDateTime.now().plusDays(7),
                new Location(55.7558f, 37.6173f),
                false,
                100,
                true,
                "Концерт в Москве"
        );

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение события пользователя - успешный сценарий")
    void getUserEvent_ValidIds_ReturnEvent() throws Exception {
        when(eventService.getUserEvent(anyLong(), anyLong())).thenReturn(eventFullDto);

        mockMvc.perform(get("/users/1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Концерт в Москве"));
    }

    @Test
    @DisplayName("Получение события пользователя - событие не найдено")
    void getUserEvent_EventNotFound_ReturnNotFound() throws Exception {
        when(eventService.getUserEvent(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Событие с id=999 не найдено"));

        mockMvc.perform(get("/users/1/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение подборки событий поблизости - успешный сценарий")
    void getUserEventsByCoordinates_withValidCoordinates_ReturnCollectionEvent() throws Exception {
        eventShortDto = EventShortDto.builder()
                .annotation("Отличный концерт в Москве")
                .category(new CategoryDto(1L, "Концерты"))
                .confirmedRequests(0L)
                .eventDate(LocalDateTime.now().plusDays(7))
                .id(1L)
                .initiator(new UserShortDto(1L, "Иван Иванов"))
                .paid(false)
                .title("Концерт в Москве")
                .views(0L)
                .distance(200.0)
                .build();

        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getUserEventsByCoordinates(anyLong(), any(PrivateEventsFilter.class), anyInt(), anyInt()))
                .thenReturn(events);

        mockMvc.perform(get("/users/1/events/nearby")
                        .param("radiusMeters", "500")
                        .param("lat", "55.7558")
                        .param("lon", "37.6173"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Концерт в Москве"))
                .andExpect(jsonPath("$[0].distance").value(200));
    }

    @Test
    @DisplayName("Получение подборки событий - пользователь не найден")
    void getUserEventsByCoordinates_UserNotFound_ReturnNotFound() throws Exception {
        when(eventService.getUserEventsByCoordinates(anyLong(), any(PrivateEventsFilter.class), anyInt(), anyInt()))
                .thenThrow(new NotFoundException("Пользователь с id= 999 не найден"));

        mockMvc.perform(get("/users/999/events/nearby")
                        .param("radiusMeters", "500")
                        .param("lat", "55.7558")
                        .param("lon", "37.6173"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение подборки событий - отрицательный ID")
    void getEventWithDistance_NegativeId_ReturnBadRequest() throws Exception {
        mockMvc.perform(get("/users/-1/events/nearby")
                        .param("radiusMeters", "500")
                        .param("lat", "55.7558")
                        .param("lon", "37.6173"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение подборки событий - обязательный параметр равен null")
    void getUserEventsByCoordinates_MissingRequiredParams_ReturnBadRequest() throws Exception {
        mockMvc.perform(get("/users/1/events/nearby")
                        .param("lat", "55.7558")
                        .param("lon", "37.6173"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение подборки событий - невалидные обязательные параметры")
    void getUserEventsByCoordinates_NotValidationParams_ReturnBadRequest() throws Exception {
        mockMvc.perform(get("/users/1/events/nearby")
                        .param("radiusMeters", "-100")
                        .param("lat", "200")
                        .param("lon", "-300"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Обновление события пользователем - успешный сценарий")
    void updateUserEvent_ValidData_ReturnUpdatedEvent() throws Exception {
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest(
                "Обновленная аннотация",
                1L,
                "Обновленное описание",
                LocalDateTime.now().plusDays(10),
                new Location(55.7558f, 37.6173f),
                true,
                200,
                false,
                UserStateAction.SEND_TO_REVIEW,
                "Обновленный концерт"
        );

        EventFullDto updatedEvent = EventFullDto.builder()
                .annotation("Обновленная аннотация")
                .category(new CategoryDto(1L, "Концерты"))
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now())
                .description("Обновленное описание")
                .eventDate(LocalDateTime.now().plusDays(10))
                .id(1L)
                .initiator(new UserShortDto(1L, "Иван Иванов"))
                .location(new Location(55.7558f, 37.6173f))
                .paid(true)
                .participantLimit(200)
                .publishedOn(null)
                .requestModeration(false)
                .state(EventState.PENDING)
                .title("Обновленный концерт")
                .views(0L)
                .build();

        when(eventService.updateUserEvent(anyLong(), anyLong(), any(UpdateEventUserRequest.class)))
                .thenReturn(updatedEvent);

        mockMvc.perform(patch("/users/1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.annotation").value("Обновленная аннотация"));
    }

    @Test
    @DisplayName("Обновление события пользователем - конфликт статуса")
    void updateUserEvent_ConflictStatus_ReturnConflict() throws Exception {
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest(
                "Обновленная аннотация",
                1L,
                "Обновленное описание",
                LocalDateTime.now().plusDays(10),
                new Location(55.7558f, 37.6173f),
                true,
                200,
                false,
                UserStateAction.SEND_TO_REVIEW,
                "Обновленный концерт"
        );

        when(eventService.updateUserEvent(anyLong(), anyLong(), any(UpdateEventUserRequest.class)))
                .thenThrow(new ConflictException("Изменить можно только ожидающие или отмененные события"));

        mockMvc.perform(patch("/users/1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());
    }
}