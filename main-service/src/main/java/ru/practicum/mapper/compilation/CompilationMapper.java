package ru.practicum.mapper.compilation;

import org.mapstruct.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.mapper.event.EventMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;

import java.util.List;
import java.util.Map;

@Mapper (componentModel = "spring", uses = EventMapper.class)
public interface CompilationMapper {
    CompilationDto toDto(Compilation compilation,@Context Map<Long,Long> viewsMap);

    @Mapping(target = "events", source = "events")
    Compilation toEntity(NewCompilationDto newCompilationDto, List<Event> events);
}
