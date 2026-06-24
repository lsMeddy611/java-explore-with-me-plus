package ru.practicum.service;

import ru.practicum.dto.compilation.CompilationDto;

import java.util.List;

public interface CompilationService {
    List<CompilationDto> getCompilations(boolean pinned, int from, int size);

    CompilationDto getCompilation(Long compilationId);
}
