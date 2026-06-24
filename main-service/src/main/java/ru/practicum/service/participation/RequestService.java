package ru.practicum.service.participation;

import ru.practicum.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId,
                                                         EventRequestStatusUpdateRequest updateRequest);
}
