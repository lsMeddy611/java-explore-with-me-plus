package ru.practicum.mapper.compilation;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.mapper.event.EventMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper (componentModel = "spring", uses = EventMapper.class)
public interface CompilationMapper {
    CompilationDto toDto(Compilation compilation,@Context Map<Long,Long> viewsMap);

    @Mapping(target = "events", source = "events", qualifiedByName = "mapIdsToEvents")
    Compilation toEntity(NewCompilationDto newCompilationDto);

    @Named("mapIdsToEvents")
    default List<Event> mapIdsToEvents(List<Long> eventIds) {
        if (eventIds == null) {
            return List.of();
        }
        return eventIds.stream()
                .map(id -> Event.builder().id(id).build())
                .collect(Collectors.toList());
    }
}
