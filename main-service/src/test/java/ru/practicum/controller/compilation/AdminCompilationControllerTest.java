package ru.practicum.controller.compilation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.StatsClient;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.service.compilation.CompilationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCompilationController.class)
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @MockBean
    private StatsClient statsClient;

    @Autowired
    private ObjectMapper objectMapper;

    private NewCompilationDto newCompilationDto;
    private CompilationDto compilationDto;

    @BeforeEach
    void setUp() {
        newCompilationDto = new NewCompilationDto(
                "Лучшие концерты 2024",
                false,
                List.of(1L, 2L, 3L)
        );

        compilationDto = new CompilationDto(
                1L,
                false,
                "Лучшие концерты 2024",
                List.of()
        );
    }

    @Test
    @DisplayName("Создание подборки - успешный сценарий")
    void createCompilation_ValidData_ReturnCreatedCompilation() throws Exception {
        when(compilationService.createCompilation(any(NewCompilationDto.class))).thenReturn(compilationDto);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCompilationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Лучшие концерты 2024"));
    }

    @Test
    @DisplayName("Создание подборки - пустой заголовок")
    void createCompilation_EmptyTitle_ReturnBadRequest() throws Exception {
        NewCompilationDto invalidDto = new NewCompilationDto("", false, List.of(1L));
        when(compilationService.createCompilation(any(NewCompilationDto.class)))
                .thenThrow(new ConflictException("Заголовок подборки не может быть пустым"));

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание подборки - дубликаты в списке событий")
    void createCompilation_DuplicateEvents_ReturnBadRequest() throws Exception {
        NewCompilationDto invalidDto = new NewCompilationDto(
                "Подборка с дубликатами",
                false,
                List.of(1L, 1L, 2L)
        );

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Обновление подборки - успешный сценарий")
    void updateCompilation_ValidData_ReturnUpdatedCompilation() throws Exception {
        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest(
                "Обновленная подборка",
                true,
                List.of(1L, 2L)
        );
        CompilationDto updatedDto = new CompilationDto(1L, true, "Обновленная подборка", List.of());

        when(compilationService.updateCompilation(anyLong(), any(UpdateCompilationRequest.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(patch("/admin/compilations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Обновленная подборка"))
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    @DisplayName("Обновление подборки - подборка не найдена")
    void updateCompilation_CompilationNotFound_ReturnNotFound() throws Exception {
        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest(
                "Обновленная подборка",
                true,
                List.of(1L)
        );

        when(compilationService.updateCompilation(anyLong(), any(UpdateCompilationRequest.class)))
                .thenThrow(new NotFoundException("Подборка с id=999 не найдена"));

        mockMvc.perform(patch("/admin/compilations/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление подборки - успешный сценарий")
    void deleteCompilation_ValidId_ReturnNoContent() throws Exception {
        doNothing().when(compilationService).deleteCompilation(anyLong());

        mockMvc.perform(delete("/admin/compilations/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Удаление подборки - подборка не найдена")
    void deleteCompilation_CompilationNotFound_ReturnNotFound() throws Exception {
        doNothing().when(compilationService).deleteCompilation(anyLong());

        mockMvc.perform(delete("/admin/compilations/999"))
                .andExpect(status().isNoContent());
    }
}