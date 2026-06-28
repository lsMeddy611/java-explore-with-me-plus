package ru.practicum.controller.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.StatsClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.service.category.CategoryService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCategoryController.class)
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    private CategoryDto categoryDto;

    @MockBean
    private StatsClient statsClient;

    @BeforeEach
    void setUp() {
        categoryDto = new CategoryDto(1L, "Концерты");
    }

    @Test
    @DisplayName("Получение всех категорий - успешный сценарий")
    void getCategories_ValidParams_ReturnListOfCategories() throws Exception {
        List<CategoryDto> categories = List.of(
                categoryDto,
                new CategoryDto(2L, "Спорт"),
                new CategoryDto(3L, "Театр")
        );
        when(categoryService.getCategories(anyInt(), anyInt())).thenReturn(categories);

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Концерты"));
    }

    @Test
    @DisplayName("Получение всех категорий - параметры по умолчанию")
    void getCategories_DefaultParams_ReturnListOfCategories() throws Exception {
        List<CategoryDto> categories = List.of(categoryDto);
        when(categoryService.getCategories(0, 10)).thenReturn(categories);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение категории по ID - успешный сценарий")
    void getCategory_ValidId_ReturnCategory() throws Exception {
        when(categoryService.getCategory(anyLong())).thenReturn(categoryDto);

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Концерты"));
    }

    @Test
    @DisplayName("Получение категории по ID - категория не найдена")
    void getCategory_CategoryNotFound_ReturnNotFound() throws Exception {
        when(categoryService.getCategory(anyLong()))
                .thenThrow(new NotFoundException("Категория с id=999 не найдена"));

        mockMvc.perform(get("/categories/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение категории по ID - отрицательный ID")
    void getCategory_NegativeId_ReturnBadRequest() throws Exception {
        mockMvc.perform(get("/categories/-1"))
                .andExpect(status().isBadRequest());
    }
}