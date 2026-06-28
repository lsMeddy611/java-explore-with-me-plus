package ru.practicum.mapper.request;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.model.Request;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "requester", source = "requester.id")
    ParticipationRequestDto toDto(Request request);
}