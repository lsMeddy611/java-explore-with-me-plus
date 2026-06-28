package ru.practicum.service.user;

import com.querydsl.core.types.Predicate;
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
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.param_objects.AdminUserFilter;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.user.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.user.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;
    private NewUserRequest newUserRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Иван Петров")
                .email("ivan@example.com")
                .build();

        userDto = new UserDto(1L, "ivan@example.com", "Иван Петров");
        newUserRequest = new NewUserRequest("ivan@example.com", "Иван Петров");
    }

    @Test
    @DisplayName("Создание пользователя - успешный сценарий")
    void createUser_ValidData_ReturnCreatedUser() {
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
        when(userMapper.toUser(newUserRequest)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = userService.createUser(newUserRequest);

        assertNotNull(result);
        assertEquals(userDto.id(), result.id());
        assertEquals(userDto.name(), result.name());
        assertEquals(userDto.email(), result.email());

        verify(userRepository).existsByEmail("ivan@example.com");
        verify(userMapper).toUser(newUserRequest);
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Создание пользователя - email уже существует")
    void createUser_DuplicateEmail_ThrowConflictException() {
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> userService.createUser(newUserRequest));

        assertEquals("Пользователь с почтой: ivan@example.com уже существует", exception.getMessage());
        verify(userRepository).existsByEmail("ivan@example.com");
        verify(userMapper, never()).toUser(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение пользователей - успешный сценарий")
    void getUsers_ValidFilter_ReturnUsersList() {
        AdminUserFilter filter = new AdminUserFilter(0, 10, List.of(1L));
        List<User> users = List.of(user);
        Page<User> page = new PageImpl<>(users);

        when(userRepository.findAll(any(Predicate.class), any(PageRequest.class)))
                .thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(userDto);

        List<UserDto> result = userService.getUsers(filter);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userDto.id(), result.get(0).id());

        verify(userRepository).findAll(any(Predicate.class), any(PageRequest.class));
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Получение пользователей - без фильтрации")
    void getUsers_NoIds_ReturnAllUsers() {
        AdminUserFilter filter = new AdminUserFilter(0, 10, List.of());
        List<User> users = List.of(user);
        Page<User> page = new PageImpl<>(users);

        when(userRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(userDto);

        List<UserDto> result = userService.getUsers(filter);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(userRepository).findAll(PageRequest.of(0, 10));
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Получение пользователей - пустой список")
    void getUsers_EmptyList_ReturnEmptyList() {
        AdminUserFilter filter = new AdminUserFilter(0, 10, List.of(1L));
        Page<User> page = new PageImpl<>(List.of());

        when(userRepository.findAll(any(Predicate.class), any(PageRequest.class)))
                .thenReturn(page);

        List<UserDto> result = userService.getUsers(filter);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository).findAll(any(Predicate.class), any(PageRequest.class));
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Удаление пользователя - успешный сценарий")
    void deleteUser_ValidId_DeleteUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Удаление пользователя - пользователь не найден")
    void deleteUser_UserNotFound_ThrowNotFoundException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.deleteUser(99L));

        assertEquals("Пользователь с ID 99 не найден", exception.getMessage());
        verify(userRepository).existsById(99L);
        verify(userRepository, never()).deleteById(any());
    }
}