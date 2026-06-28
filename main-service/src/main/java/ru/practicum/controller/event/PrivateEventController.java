package ru.practicum.controller.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.service.event.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
@Validated
@Slf4j
public class PrivateEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getUserEvents(@PathVariable Long userId,
                                                             @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                             @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /users/{}/events: from={}, size={}", userId, from, size);
        return ResponseEntity.status(HttpStatus.OK)
                .body(eventService.getUserEvents(userId, from, size));
    }

    @PostMapping
    public ResponseEntity<EventFullDto> createEvent(@PathVariable Long userId, @Valid @RequestBody NewEventDto newEventDto) {
        log.info("POST /users/{}/events: {}", userId, newEventDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(userId, newEventDto));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getUserEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}", userId, eventId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(eventService.getUserEvent(userId, eventId));
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateUserEvent(@PathVariable Long userId, @PathVariable Long eventId,
                                                        @Valid @RequestBody UpdateEventUserRequest updateRequest) {
        log.info("PATCH /users/{}/events/{}: {}", userId, eventId, updateRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(eventService.updateUserEvent(userId, eventId, updateRequest));
    }

}