package ru.practicum.controller.compilation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.StatsClient;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.service.compilation.CompilationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicCompilationController.class)
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @MockBean
    private StatsClient statsClient;

    private CompilationDto compilationDto;

    @BeforeEach
    void setUp() {
        compilationDto = new CompilationDto(
                1L,
                true,
                "Лучшие концерты 2024",
                List.of()
        );
    }

    @Test
    @DisplayName("Получение всех подборок - успешный сценарий")
    void getCompilations_ValidParams_ReturnListOfCompilations() throws Exception {
        List<CompilationDto> compilations = List.of(
                compilationDto,
                new CompilationDto(2L, false, "Спортивные события", List.of())
        );

        when(compilationService.getCompilations(anyBoolean(), anyInt(), anyInt()))
                .thenReturn(compilations);

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение всех подборок - параметры по умолчанию")
    void getCompilations_DefaultParams_ReturnListOfCompilations() throws Exception {
        List<CompilationDto> compilations = List.of(compilationDto);
        when(compilationService.getCompilations(false, 0, 10))
                .thenReturn(compilations);

        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение подборки по ID - успешный сценарий")
    void getCompilation_ValidId_ReturnCompilation() throws Exception {
        when(compilationService.getCompilation(anyLong())).thenReturn(compilationDto);

        mockMvc.perform(get("/compilations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Лучшие концерты 2024"));
    }

    @Test
    @DisplayName("Получение подборки по ID - подборка не найдена")
    void getCompilation_CompilationNotFound_ReturnNotFound() throws Exception {
        when(compilationService.getCompilation(anyLong()))
                .thenThrow(new NotFoundException("Подборка с id=999 не найдена"));

        mockMvc.perform(get("/compilations/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение подборки по ID - отрицательный ID")
    void getCompilation_NegativeId_ReturnBadRequest() throws Exception {
        mockMvc.perform(get("/compilations/-1"))
                .andExpect(status().isBadRequest());
    }
}