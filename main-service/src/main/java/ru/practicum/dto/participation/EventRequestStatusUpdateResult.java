package ru.practicum.dto.participation;

import java.util.List;

public record EventRequestStatusUpdateResult(
        List<ParticipationRequestDto> confirmedRequests,
        List<ParticipationRequestDto> rejectedRequests
) {}