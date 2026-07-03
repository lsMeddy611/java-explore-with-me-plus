package ru.practicum.dto.event.projection;

import ru.practicum.model.Category;
import ru.practicum.model.User;

import java.time.LocalDateTime;

public interface EventShortProjection {
    String getAnnotation();

    Category getCategory();

    Long getConfirmedRequests();

    LocalDateTime getEventDate();

    Long getId();

    User getInitiator();

    Boolean getPaid();

    String getTitle();

    Double getDistance();
}