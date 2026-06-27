package ru.practicum.service.compilation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.compilation.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.compilation.CompilationRepository;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.service.event.EventService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Получение компиляции pinned: {}, from: {}, size: {}", pinned, from, size);
        List<Long> compilationIds = compilationRepository.getCompilationIds(pinned, from, size);
        if (compilationIds.isEmpty()) {
            log.debug("Не найдено ни одной подборки событий");
            return List.of();
        }

        List<Compilation> compilations = compilationRepository.getCompilations(compilationIds);

        Set<Long> eventIds = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .map(Event::getId)
                .collect(Collectors.toSet());

        Map<Long, Long> viewsMap = eventService.getViews(new ArrayList<>(eventIds));
        log.debug("Получено {} подборок событий", compilations.size());

        return compilations.stream()
                .map(compilation -> compilationMapper.toDto(compilation, viewsMap))
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilation(long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка событий с указанным ID не найдена"));

        log.info("Получена подборка событий с id = {}", compilation.getId());

        Set<Long> eventIds = compilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        Map<Long, Long> views = eventService.getViews(new ArrayList<>(eventIds));

        return compilationMapper.toDto(compilation, views);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = compilationMapper.toEntity(newCompilationDto);
        List<Event> events = eventRepository.findAllById(newCompilationDto.events());
        compilation.setEvents(events);
        Compilation savedCompilation = compilationRepository.save(compilation);

        Map<Long, Long> views = eventService.getViews(newCompilationDto.events());
        log.info("Создана новая подборка событий id = {}", savedCompilation.getId());
        return compilationMapper.toDto(savedCompilation, views);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка событий с указанным ID не найдена"));

        Set<Long> eventIds = Set.of();

        if (updateCompilationRequest.title() != null) {
            compilation.setTitle(updateCompilationRequest.title());
        }

        if (updateCompilationRequest.pinned() != null) {
            compilation.setPinned(updateCompilationRequest.pinned());
        }

        if (updateCompilationRequest.events() != null) {
            List<Event> updatedEvents = eventRepository.findAllById(updateCompilationRequest.events());
            if (updatedEvents.size() != updateCompilationRequest.events().size()) {
                log.warn("При обновлении подборки Id = {} не найдены события из запроса: {}", compId,
                        updateCompilationRequest.events());
                throw new NotFoundException("Одно или несколько событий не найдены");
            }
            compilation.setEvents(updatedEvents);
            eventIds = updatedEvents.stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());
        }

        Map<Long, Long> views = eventService.getViews(new ArrayList<>(eventIds));

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Обновлена подборка событий id = {}", updatedCompilation.getId());
        return compilationMapper.toDto(updatedCompilation, views);
    }

    @Override
    @Transactional
    public void deleteCompilation(long compId) {
        if (!compilationRepository.existsById(compId)) {
            log.warn("Подборка событий с указанным Id = {} не найдена", compId);
            throw new NotFoundException("Подборка событий с указанным ID не найдена");
        }
        compilationRepository.deleteById(compId);
    }
}

