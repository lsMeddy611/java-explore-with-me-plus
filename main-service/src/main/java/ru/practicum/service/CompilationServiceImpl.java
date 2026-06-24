package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.сompilation.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.repository.CompilationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public List<CompilationDto> getCompilations(boolean pinned, int from, int size) {
        List<Long> compilationIds = compilationRepository.getCompilationIds(pinned, from, size);
        if (compilationIds.isEmpty()) {
            log.info("Не найдено ни одной подборки событий");
            return List.of();
        }

        List<CompilationDto> compilations = compilationRepository.getCompilations(compilationIds).stream()
                .map(compilation -> compilationMapper.toDto(compilation))
                .collect(Collectors.toList());

        log.info("Получено {} подборок событий", compilations.size());
        return compilations;
    }

    @Override
    public CompilationDto getCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка событий с указанным ID не найдена"));

        log.info("Получена подборка событий с id = {}", compilation.getId());
        return compilationMapper.toDto(compilation);
    }
}

