package ru.practicum.service.compilation;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.compilation.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.compilation.CompilationRepository;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.service.event.EventService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompilationServiceTest {
    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CompilationMapper compilationMapper;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private CompilationDto compilationDto;
    private NewCompilationDto newCompilationDto;
    private UpdateCompilationRequest updateRequest;
    private List<Event> events;
    private List<Long> eventIds;
    private HashMap<Long, Long> viewsMap;
    private EventShortDto shortEvent;

    @BeforeEach
    void setUp() {
        events = List.of(
                Event.builder().id(1L).build(),
                Event.builder().id(2L).build()
        );
        compilation = Compilation.builder()
                .id(10L)
                .title("Test Compilation")
                .pinned(true)
                .events(new ArrayList<>(events))
                .build();
        shortEvent = new EventShortDto(
                "Аннотация",
                new CategoryDto(1L, "Cat"),
                0L,
                LocalDateTime.now().plusDays(1),
                1L,
                new UserShortDto(1L, "User"),
                false,
                "Title",
                100L
        );
        compilationDto = new CompilationDto(10L, true, "Test Compilation", List.of(shortEvent));
        newCompilationDto = new NewCompilationDto("New", true, List.of(1L, 2L));
        updateRequest = new UpdateCompilationRequest("Updated", false, List.of(1L));
    }

    @Test
    void shouldReturnEmptyList_whenNoIds() {
        when(compilationRepository.getCompilationIds(Mockito.anyBoolean(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(List.of());

        List<CompilationDto> result = compilationService.getCompilations(true, 10, 5);
        assertThat(result).isEmpty();

        verify(compilationRepository, times(1)).getCompilationIds(Mockito.anyBoolean(),
                Mockito.anyInt(), Mockito.anyInt());

        verify(compilationRepository, never()).getCompilations(anyList());
        verify(eventService, never()).getViews(anyList());
        verify(compilationMapper, never()).toDto(any(), anyMap());
    }

    @Test
    void shouldReturnCompilationsDto_whenIdsExist() {
        int from = 10;
        int size = 5;
        boolean pinned = true;

        eventIds = List.of(10L, 20L);
        viewsMap = new HashMap<>();
        viewsMap.put(1L, 100L);
        viewsMap.put(2L, 200L);

        when(compilationRepository.getCompilationIds(eq(pinned), eq(from), eq(size)))
                .thenReturn(eventIds);

        when(compilationRepository.getCompilations(eventIds))
                .thenReturn(List.of(compilation));

        when(eventService.getViews(anyList()))
                .thenReturn(viewsMap);

        when(compilationMapper.toDto(compilation, viewsMap))
                .thenReturn(compilationDto);

        List<CompilationDto> result = compilationService.getCompilations(true, from, size);

        assertThat(result).containsExactly(compilationDto);

        verify(compilationRepository, times(1)).getCompilationIds(true, from, size);
        verify(compilationRepository, times(1)).getCompilations(eventIds);
        verify(eventService, times(1)).getViews(anyList());
        verify(compilationMapper, times(1)).toDto(compilation, viewsMap);
    }

    @Test
    void shouldPassCorrectParametersToRepository() {
        when(compilationRepository.getCompilationIds(anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of());

        compilationService.getCompilations(true, 0, 10);

        verify(compilationRepository).getCompilationIds(eq(true), eq(0), eq(10));
    }

    @Test
    void shouldHandleCompilationsDtoWithoutEvents() {
        Compilation comp = Compilation.builder()
                .id(10L)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of())
                .build();

        CompilationDto compDto =
                new CompilationDto(10L, true, "Test Compilation", List.of());

        eventIds = List.of(10L, 20L);

        when(compilationRepository.getCompilationIds(Mockito.anyBoolean(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(eventIds);

        when(compilationRepository.getCompilations(eventIds))
                .thenReturn(List.of(comp));

        when(eventService.getViews(Collections.emptyList()))
                .thenReturn(Collections.emptyMap());

        when(compilationMapper.toDto(comp, Collections.emptyMap()))
                .thenReturn(compDto);

        List<CompilationDto> result = compilationService.getCompilations(true, 10, 5);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().events()).isEmpty();

        verify(compilationMapper).toDto(comp, Collections.emptyMap());
    }

    @Test
    void shouldReturnCompilationDtoWhenCompilationIdExists() {
        viewsMap = new HashMap<>();
        viewsMap.put(1L, 100L);
        viewsMap.put(2L, 200L);

        when(compilationRepository.findById(anyLong()))
                .thenReturn(Optional.of(compilation));

        when(eventService.getViews(anyList()))
                .thenReturn(viewsMap);

        when(compilationMapper.toDto(compilation, viewsMap))
                .thenReturn(compilationDto);

        CompilationDto result = compilationService.getCompilation(10L);

        assertThat(result).isEqualTo(compilationDto);

        verify(compilationRepository, times(1)).findById(anyLong());
        verify(eventService, times(1)).getViews(anyList());
        verify(compilationMapper, times(1)).toDto(compilation, viewsMap);

    }

    @Test
    void shouldReturnBadRequestWhenCompilationIdNotFound() {
        when(compilationRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.getCompilation(10L));

        assertEquals("Подборка событий с указанным ID не найдена", exception.getMessage());

        verify(eventService, never()).getViews(any());
    }

    @Test
    void shouldPassCorrectCompilationIdToRepository() {
        when(compilationRepository.findById(anyLong()))
                .thenReturn(Optional.of(compilation));

        compilationService.getCompilation(10L);

        verify(compilationRepository).findById(eq(10L));
    }

    @Test
    void shouldHandleCompilationDtoWithoutEvents() {
        Compilation comp = Compilation.builder()
                .id(10L)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of())
                .build();

        CompilationDto compDto =
                new CompilationDto(10L, true, "Test Compilation", List.of());

        when(compilationRepository.findById(anyLong()))
                .thenReturn(Optional.of(comp));

        when(eventService.getViews(Collections.emptyList()))
                .thenReturn(Collections.emptyMap());

        when(compilationMapper.toDto(comp, Collections.emptyMap()))
                .thenReturn(compDto);

        CompilationDto result = compilationService.getCompilation(5L);

        assertThat(result.events()).isEmpty();

        verify(compilationMapper).toDto(comp, Collections.emptyMap());
    }

    @Test
    void shouldCreateCompilationWhenValidDataProvided() {
        viewsMap = new HashMap<>();
        viewsMap.put(1L, 100L);
        viewsMap.put(2L, 200L);

        when(eventRepository.findAllById(anyList()))
                .thenReturn(events);

        when(compilationMapper.toEntity(Mockito.any(), Mockito.anyList()))
                .thenReturn(compilation);

        when(compilationRepository.save(Mockito.any()))
                .thenReturn(compilation);

        when(eventService.getViews(Mockito.anyList()))
                .thenReturn(viewsMap);

        when(compilationMapper.toDto(compilation, viewsMap))
                .thenReturn(compilationDto);

        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        assertThat(result).isEqualTo(compilationDto);

        List<Long> eventIds = compilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        verify(eventRepository, times(1)).findAllById(anyList());
        verify(compilationMapper, times(1)).toEntity(newCompilationDto, events);
        verify(compilationRepository, times(1)).save(compilation);
        verify(eventService, times(1)).getViews(eventIds);
        verify(compilationMapper, times(1)).toDto(compilation, viewsMap);
    }

    @Test
    void shouldCreateCompilationWithEmptyEventList() {
        NewCompilationDto newCompDto = new NewCompilationDto("Test Compilation", true, List.of());

        Compilation comp = Compilation.builder()
                .id(10L)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of())
                .build();

        CompilationDto compDto =
                new CompilationDto(10L, true, "Test Compilation", List.of());

        when(compilationMapper.toEntity(newCompDto, Collections.emptyList()))
                .thenReturn(comp);

        when(compilationRepository.save(comp))
                .thenReturn(comp);

        when(eventService.getViews(Collections.emptyList()))
                .thenReturn(Collections.emptyMap());

        when(compilationMapper.toDto(comp, Collections.emptyMap()))
                .thenReturn(compDto);

        CompilationDto result = compilationService.createCompilation(newCompDto);

        assertThat(result.events()).isEmpty();

        verify(compilationMapper).toDto(comp, Collections.emptyMap());
        verify(eventRepository, never()).findAllById(anyList());
    }

    @Test
    void shouldPassCorrectNewCompilationToRepository() {
        NewCompilationDto dto = new NewCompilationDto("Test Compilation", true, List.of());

        Compilation comp = Compilation.builder()
                .title("Test Compilation")
                .pinned(true)
                .events(List.of())
                .build();

        CompilationDto compDto = new CompilationDto(1L, true, "Test Compilation", List.of());

        when(compilationMapper.toEntity(dto, Collections.emptyList()))
                .thenReturn(comp);

        when(compilationRepository.save(any(Compilation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(eventService.getViews(Collections.emptyList()))
                .thenReturn(Collections.emptyMap());

        when(compilationMapper.toDto(comp, Collections.emptyMap()))
                .thenReturn(compDto);

        CompilationDto result = compilationService.createCompilation(dto);

        verify(compilationRepository).save(comp);

        assertThat(result.title()).isEqualTo("Test Compilation");
        assertThat(result.pinned()).isTrue();
        assertThat(result.events()).isEmpty();
    }

    @Test
    void shouldThrowNotFoundExceptionWhenSomeEventsMissing() {
        List<Long> eventIds = List.of(1L, 2L, 3L);
        NewCompilationDto newComDto = new NewCompilationDto("New", true, eventIds);

        when(eventRepository.findAllById(eventIds)).thenReturn(events);

        assertThatThrownBy(() -> compilationService.createCompilation(newComDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Не все события найдены");

        verify(compilationMapper, never()).toEntity(any(), any());
        verify(compilationRepository, never()).save(any());
        verify(eventService, never()).getViews(anyList());
        verify(compilationMapper, never()).toDto(any(), any());
    }


/*          compilationDto = new CompilationDto(10L,true,"Test Compilation", List.of(shortEvent));
        newCompilationDto = new NewCompilationDto("New", true, List.of(1L, 2L));

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        List<Event> events = loadingEvents(newCompilationDto.events());
        Compilation compilation = compilationMapper.toEntity(newCompilationDto, events);
        Compilation savedCompilation = compilationRepository.save(compilation);

        Set<Long> eventIds = savedCompilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toSet());

        Map<Long, Long> views = eventService.getViews(new ArrayList<>(eventIds));
        log.info("Создана новая подборка событий id = {}", savedCompilation.getId());
        return compilationMapper.toDto(savedCompilation, views);
    }*/

}
