package ru.practicum.mapper.user;

import org.mapstruct.Mapper;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(NewUserRequest newUserRequest);

    UserDto toDto(User user);

    UserShortDto toShortDto(User user);
}