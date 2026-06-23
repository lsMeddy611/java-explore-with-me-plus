package ru.practicum.service.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.category.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.category.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        return categoryRepository.findAll(PageRequest.of(from / size, size))
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категории с таким id " + catId + " не найдено"));
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        if (categoryRepository.existsByName(newCategoryDto.name())) {
            throw new ConflictException("Категория с именем '" + newCategoryDto.name() + "' уже существует");
        }
        Category category = categoryMapper.toEntity(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
        if (!category.getName().equals(categoryDto.name()) &&
                categoryRepository.existsByName(categoryDto.name())) {
            throw new ConflictException("Категория с именем '" + categoryDto.name() + "' уже существует");
        }

        category.setName(categoryDto.name());
        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
        categoryRepository.delete(category);
    }
}
