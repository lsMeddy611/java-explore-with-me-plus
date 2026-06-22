package ru.practicum.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.UserNotFoundException;
import ru.practicum.mapper.user.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info("Создание пользователя user={}", newUserRequest);
        User user = userMapper.toUser(newUserRequest);
        user = userRepository.save(user);
        log.info("Пользователь успешно создан id= {}", user.getId());

        return userMapper.toDto(user);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.info("Получение информации о всех пользователях");
        List<User> users = userRepository.findAll();
        return users
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public void deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        } else {
            throw new UserNotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }
}
