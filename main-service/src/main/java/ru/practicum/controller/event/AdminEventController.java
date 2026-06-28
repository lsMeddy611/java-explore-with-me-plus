package ru.practicum.controller.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.dto.event.param_objects.AdminEventsFilter;
import ru.practicum.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminEventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventFullDto>> getAdminEvents(@ModelAttribute AdminEventsFilter filter) {
        log.info("GET /admin/events");
        return ResponseEntity.status(HttpStatus.OK).body(eventService.getAdminEvents(filter));
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateAdminEvent(
            @Positive @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventAdminRequest request) {
        log.info("PATCH /admin/events/{}", eventId);
        return ResponseEntity.status(HttpStatus.OK).body(eventService.updateAdminEvent(eventId, request));
    }
}