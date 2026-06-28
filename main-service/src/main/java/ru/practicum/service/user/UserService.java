package ru.practicum.service.user;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.param_objects.AdminUserFilter;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(AdminUserFilter filter);

    void deleteUser(Long userId);
}