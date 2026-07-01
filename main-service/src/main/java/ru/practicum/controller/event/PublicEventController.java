package ru.practicum.controller.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import ru.practicum.EndpointHit;
import ru.practicum.EndpointHitInfo;
import ru.practicum.StatsClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.param_objects.PublicEventsFilter;
import ru.practicum.service.event.EventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventController {
    private final EventService eventService;
    private final StatsClient statsClient;

    private static final String APP_NAME = "ewm-main-service";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEventsByFilter(@ModelAttribute @Valid PublicEventsFilter filter,
                                                                 HttpServletRequest request) {
        log.info("/GET /events");
        EndpointHitInfo endpointHitInfo = createEndpointHitInfo(request);

        return ResponseEntity.status(HttpStatus.OK).body(eventService.getPublishedEvents(filter, endpointHitInfo));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getEventById(@Positive @PathVariable("eventId") Long eventId,
                                                     @RequestParam(required = false) @Min(-90) @Max(90) Double lat,
                                                     @RequestParam(required = false) @Min(-180) @Max(180) Double lon,
                                                     HttpServletRequest request) {
        log.info("GET /events/{}", eventId);
        EndpointHit endpointHit = createEndpointHit(request);

        try {
            statsClient.saveHit(endpointHit);
        } catch (RestClientException e) {
            log.error("Не удалось сохранить информацию о статистике endpointHit= {}", endpointHit);
        }

        return ResponseEntity.status(HttpStatus.OK).body(eventService.getEventById(eventId, lat, lon));
    }

    private EndpointHit createEndpointHit(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        String createdOn = LocalDateTime.now().format(formatter);

        return new EndpointHit(null, APP_NAME, uri, ip, createdOn);
    }

    private EndpointHitInfo createEndpointHitInfo(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        String createdOn = LocalDateTime.now().format(formatter);

        return new EndpointHitInfo(APP_NAME, uri, ip, createdOn);
    }
}