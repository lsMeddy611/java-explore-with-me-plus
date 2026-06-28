package ru.practicum.controller.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.service.request.RequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/requests")
@Slf4j
public class PrivateRequestController {

    private final RequestService requestService;

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable Long userId) {
        log.info("GET /users/{}/requests", userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(requestService.getUserRequests(userId));
    }

    @PostMapping
    public ResponseEntity<ParticipationRequestDto> addParticipationRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {
        log.info("POST /users/{}/requests?eventId={}", userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.addParticipationRequest(userId, eventId));
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel", userId, requestId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(requestService.cancelRequest(userId, requestId));
    }
}