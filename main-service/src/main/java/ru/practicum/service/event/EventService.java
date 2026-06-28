package ru.practicum.service.event;

import ru.practicum.EndpointHitInfo;
import ru.practicum.dto.event.*;
import ru.practicum.dto.event.param_objects.AdminEventsFilter;
import ru.practicum.dto.event.param_objects.PublicEventsFilter;

import java.util.List;
import java.util.Map;

public interface EventService {
    EventFullDto getEventById(Long eventId);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<EventShortDto> getPublishedEvents(PublicEventsFilter filter, EndpointHitInfo endpointHitInfo);

    List<EventFullDto> getAdminEvents(AdminEventsFilter filter);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);

    Map<Long, Long> getViews(List<Long> eventIds);
}