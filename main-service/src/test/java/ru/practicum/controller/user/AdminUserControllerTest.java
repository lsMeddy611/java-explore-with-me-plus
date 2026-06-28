package ru.practicum.controller.user;

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
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.param_objects.AdminUserFilter;
import ru.practicum.exception.ConflictException;
import ru.practicum.service.user.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private StatsClient statsClient;

    @Autowired
    private ObjectMapper objectMapper;

    private NewUserRequest newUserRequest;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        newUserRequest = new NewUserRequest("ivan@example.com", "Иван Иванов");
        userDto = new UserDto(1L, "ivan@example.com", "Иван Иванов");
    }

    @Test
    @DisplayName("Создание пользователя - успешный сценарий")
    void createUser_ValidData_ReturnCreatedUser() throws Exception {
        when(userService.createUser(any(NewUserRequest.class))).thenReturn(userDto);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.name").value("Иван Иванов"));
    }

    @Test
    @DisplayName("Создание пользователя - неверный email")
    void createUser_InvalidEmail_ReturnBadRequest() throws Exception {
        NewUserRequest invalidRequest = new NewUserRequest("invalid-email", "Иван Иванов");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание пользователя - пустое имя")
    void createUser_EmptyName_ReturnBadRequest() throws Exception {
        NewUserRequest invalidRequest = new NewUserRequest("ivan@example.com", "");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание пользователя - дубликат email")
    void createUser_DuplicateEmail_ReturnConflict() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new ConflictException("Пользователь с почтой ivan@example.com уже существует"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Получение всех пользователей - успешный сценарий")
    void getUsers_ValidFilter_ReturnListOfUsers() throws Exception {
        List<UserDto> users = List.of(userDto);
        when(userService.getUsers(any(AdminUserFilter.class))).thenReturn(users);

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение всех пользователей - параметры по умолчанию")
    void getUsers_DefaultParams_ReturnListOfUsers() throws Exception {
        List<UserDto> users = List.of(userDto);
        when(userService.getUsers(any(AdminUserFilter.class))).thenReturn(users);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение всех пользователей - пустой список")
    void getUsers_EmptyList_ReturnEmptyList() throws Exception {
        when(userService.getUsers(any(AdminUserFilter.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Удаление пользователя - успешный сценарий")
    void deleteUser_ValidId_ReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(anyLong());

        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Удаление пользователя - пользователь не найден")
    void deleteUser_UserNotFound_ReturnNotFound() throws Exception {
        doNothing().when(userService).deleteUser(anyLong());

        mockMvc.perform(delete("/admin/users/999"))
                .andExpect(status().isNoContent());
    }
}