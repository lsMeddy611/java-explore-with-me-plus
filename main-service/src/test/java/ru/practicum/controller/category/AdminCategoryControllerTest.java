package ru.practicum.controller.category;

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
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.service.category.CategoryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private StatsClient statsClient;


    @Autowired
    private ObjectMapper objectMapper;

    private NewCategoryDto newCategoryDto;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        newCategoryDto = new NewCategoryDto("Концерты");

        categoryDto = new CategoryDto(1L, "Концерты");
    }

    @Test
    @DisplayName("Создание категории - успешный сценарий")
    void createCategory_ValidData_ReturnCreatedCategory() throws Exception {
        when(categoryService.createCategory(any(NewCategoryDto.class))).thenReturn(categoryDto);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategoryDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Концерты"));
    }

    @Test
    @DisplayName("Создание категории - пустое имя")
    void createCategory_EmptyName_ReturnBadRequest() throws Exception {
        NewCategoryDto invalidDto = new NewCategoryDto("");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание категории - имя превышает 50 символов")
    void createCategory_NameTooLong_ReturnBadRequest() throws Exception {
        String longName = "Очень длинное название категории которое превышает допустимый лимит в пятьдесят символов";
        NewCategoryDto invalidDto = new NewCategoryDto(longName);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание категории - дубликат имени")
    void createCategory_DuplicateName_ReturnConflict() throws Exception {
        when(categoryService.createCategory(any(NewCategoryDto.class)))
                .thenThrow(new ConflictException("Категория с таким именем уже существует"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategoryDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Обновление категории - успешный сценарий")
    void updateCategory_ValidData_ReturnUpdatedCategory() throws Exception {
        CategoryDto updatedDto = new CategoryDto(1L, "Концерты и шоу");
        when(categoryService.updateCategory(anyLong(), any(CategoryDto.class))).thenReturn(updatedDto);

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Концерты и шоу"));
    }

    @Test
    @DisplayName("Обновление категории - категория не найдена")
    void updateCategory_CategoryNotFound_ReturnNotFound() throws Exception {
        CategoryDto updateDto = new CategoryDto(999L, "Несуществующая категория");
        when(categoryService.updateCategory(anyLong(), any(CategoryDto.class)))
                .thenThrow(new NotFoundException("Категория с id=999 не найдена"));

        mockMvc.perform(patch("/admin/categories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление категории - пустое имя")
    void updateCategory_EmptyName_ReturnBadRequest() throws Exception {
        CategoryDto invalidDto = new CategoryDto(1L, "");

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Удаление категории - успешный сценарий")
    void deleteCategory_ValidId_ReturnNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory(anyLong());

        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Удаление категории - категория не найдена")
    void deleteCategory_CategoryNotFound_ReturnNotFound() throws Exception {
        doNothing().when(categoryService).deleteCategory(anyLong());

        mockMvc.perform(delete("/admin/categories/999"))
                .andExpect(status().isNoContent());
    }
}