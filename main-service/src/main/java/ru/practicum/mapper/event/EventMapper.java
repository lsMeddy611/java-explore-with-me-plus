package ru.practicum.mapper.event;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.dto.location.Location;
import ru.practicum.mapper.category.CategoryMapper;
import ru.practicum.mapper.user.UserMapper;
import ru.practicum.model.Event;

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

    @Mapping(target = "location", expression = "java(toLocation(event))")
    EventFullDto toFullDto(Event event, Long views);

    EventShortDto toShortDto(Event event, Long views);

    default Location toLocation(Event event) {
        if (event.getLat() == null || event.getLon() == null) {
            return null;
        }
        return new Location(event.getLat().floatValue(), event.getLon().floatValue());
    }
}
