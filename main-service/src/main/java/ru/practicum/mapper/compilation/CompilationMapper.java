package ru.practicum.mapper.compilation;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.mapper.event.EventMapper;
import ru.practicum.model.Compilation;

import java.util.Map;

@Mapper (componentModel = "spring", uses = EventMapper.class)
public interface CompilationMapper {
    CompilationDto toDto(Compilation compilation,@Context Map<Long,Long> viewsMap);

    Compilation toEntity(NewCompilationDto newCompilationDto);
}
