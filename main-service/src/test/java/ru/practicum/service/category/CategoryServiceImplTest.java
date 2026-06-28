package ru.practicum.service.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.category.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.category.CategoryRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryDto categoryDto;
    private NewCategoryDto newCategoryDto;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Концерты")
                .build();

        categoryDto = new CategoryDto(1L, "Концерты");
        newCategoryDto = new NewCategoryDto("Концерты");
    }

    @Test
    @DisplayName("Получение всех категорий - успешный сценарий")
    void getCategories_ValidParams_ReturnCategoriesList() {
        List<Category> categories = List.of(category);
        Page<Category> page = new PageImpl<>(categories);

        when(categoryRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);
        when(categoryMapper.toDto(any(Category.class))).thenReturn(categoryDto);

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(categoryDto.id(), result.get(0).id());
        assertEquals(categoryDto.name(), result.get(0).name());

        verify(categoryRepository).findAll(PageRequest.of(0, 10));
        verify(categoryMapper, times(1)).toDto(any(Category.class));
    }

    @Test
    @DisplayName("Получение категории по ID - успешный сценарий")
    void getCategory_ValidId_ReturnCategoryDto() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.getCategory(1L);

        assertNotNull(result);
        assertEquals(categoryDto.id(), result.id());
        assertEquals(categoryDto.name(), result.name());

        verify(categoryRepository).findById(1L);
        verify(categoryMapper).toDto(category);
    }

    @Test
    @DisplayName("Получение категории по ID - категория не найдена")
    void getCategory_CategoryNotFound_ThrowNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.getCategory(99L));

        assertEquals("Категория с id 99 не найдена", exception.getMessage());
        verify(categoryRepository).findById(99L);
        verify(categoryMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Создание категории - успешный сценарий")
    void createCategory_ValidData_ReturnCreatedCategory() {
        when(categoryRepository.existsByName("Концерты")).thenReturn(false);
        when(categoryMapper.toEntity(newCategoryDto)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.createCategory(newCategoryDto);

        assertNotNull(result);
        assertEquals(categoryDto.id(), result.id());
        assertEquals(categoryDto.name(), result.name());

        verify(categoryRepository).existsByName("Концерты");
        verify(categoryMapper).toEntity(newCategoryDto);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toDto(category);
    }

    @Test
    @DisplayName("Создание категории - категория с таким именем уже существует")
    void createCategory_DuplicateName_ThrowConflictException() {
        when(categoryRepository.existsByName("Концерты")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> categoryService.createCategory(newCategoryDto));

        assertEquals("Категория с именем 'Концерты' уже существует", exception.getMessage());
        verify(categoryRepository, times(2)).existsByName("Концерты");
        verify(categoryMapper, never()).toEntity(any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление категории - успешный сценарий")
    void updateCategory_ValidData_ReturnUpdatedCategory() {
        Category updatedCategory = Category.builder()
                .id(1L)
                .name("Театры")
                .build();
        CategoryDto updatedCategoryDto = new CategoryDto(1L, "Театры");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Театры")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);
        when(categoryMapper.toDto(updatedCategory)).thenReturn(updatedCategoryDto);

        CategoryDto result = categoryService.updateCategory(1L, updatedCategoryDto);

        assertNotNull(result);
        assertEquals(updatedCategoryDto.id(), result.id());
        assertEquals(updatedCategoryDto.name(), result.name());

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).existsByName("Театры");
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).toDto(updatedCategory);
    }

    @Test
    @DisplayName("Обновление категории - имя не изменяется")
    void updateCategory_SameName_DoNotValidate() {
        CategoryDto sameNameDto = new CategoryDto(1L, "Концерты");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(sameNameDto);

        CategoryDto result = categoryService.updateCategory(1L, sameNameDto);

        assertNotNull(result);
        assertEquals(sameNameDto.id(), result.id());
        assertEquals(sameNameDto.name(), result.name());

        verify(categoryRepository).findById(1L);
        verify(categoryRepository, never()).existsByName(anyString());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Обновление категории - категория не найдена")
    void updateCategory_CategoryNotFound_ThrowNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.updateCategory(99L, categoryDto));

        assertEquals("Категория с id 99 не найдена", exception.getMessage());
        verify(categoryRepository).findById(99L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Удаление категории - успешный сценарий")
    void deleteCategory_ValidId_DeleteCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("Удаление категории - категория не найдена")
    void deleteCategory_CategoryNotFound_ThrowNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.deleteCategory(99L));

        assertEquals("Категория с id 99 не найдена", exception.getMessage());
        verify(categoryRepository).findById(99L);
        verify(categoryRepository, never()).delete(any());
    }
}