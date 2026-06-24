package ru.practicum.mapper.сompilation;

import org.mapstruct.Mapper;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.model.Compilation;

@Mapper (componentModel = "spring", uses = EventMapper.class)
public interface CompilationMapper {
    CompilationDto toDto(Compilation compilation);

    Compilation toEntity(CompilationDto dto);
}
