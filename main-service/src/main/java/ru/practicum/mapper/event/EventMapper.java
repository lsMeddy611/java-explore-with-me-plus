package ru.practicum.mapper.event;

import org.mapstruct.*;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.dto.event.*;
import ru.practicum.dto.location.Location;
import ru.practicum.mapper.category.CategoryMapper;
import ru.practicum.mapper.user.UserMapper;
import ru.practicum.model.Event;

import java.util.Map;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lat", source = "location.lat")
    @Mapping(target = "lon", source = "location.lon")
    Event toEntity(NewEventDto dto);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "lat", source = "location.lat")
    @Mapping(target = "lon", source = "location.lon")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateEventUserRequest dto, @MappingTarget Event event);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "lat", source = "location.lat")
    @Mapping(target = "lon", source = "location.lon")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromAdminDto(UpdateEventAdminRequest dto, @MappingTarget Event event);

    @Mapping(target = "location", expression = "java(toLocation(event))")
    EventFullDto toFullDto(Event event, Long views);

    @Mapping(target = "views", expression = "java(viewsMap.get(event.getId()))")
    EventShortDto toShortDto(Event event, @Context Map<Long, Long> viewsMap);

    @Mapping(target = "views", expression = "java(views)")
    EventShortDto toShortDto(Event event, @Context Long views);

    default Location toLocation(Event event) {
        if (event.getLat() == null || event.getLon() == null) {
            return null;
        }
        return new Location(event.getLat().floatValue(), event.getLon().floatValue());
    }
}