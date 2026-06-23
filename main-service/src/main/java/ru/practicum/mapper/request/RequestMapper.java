package ru.practicum.mapper.request;

import org.mapstruct.Mapper;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.model.Request;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    ParticipationRequestDto toDto(Request request);
}
