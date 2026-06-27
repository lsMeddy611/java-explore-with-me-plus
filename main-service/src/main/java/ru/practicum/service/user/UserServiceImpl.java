package ru.practicum.service.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.QUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.param_objects.AdminUserFilter;
import ru.practicum.mapper.user.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.user.UserRepository;

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
        emailExists(newUserRequest.email());
        User user = userMapper.toUser(newUserRequest);
        user = userRepository.save(user);
        log.info("Пользователь успешно создан id= {}", user.getId());

        return userMapper.toDto(user);
    }

    @Override
    public List<UserDto> getUsers(AdminUserFilter filter) {
        log.info("Получение пользователей с параметрами: {}", filter);
        int page = filter.from() / filter.size();
        Pageable pageable = PageRequest.of(page, filter.size());
        List<User> users;

        if (filter.ids().isEmpty()) {
            users = userRepository.findAll(pageable).getContent();
        } else {
            Predicate predicate = buildPredicate(filter);
            users = userRepository.findAll(predicate, pageable).getContent();
        }

        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public void deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        } else {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }

    private Predicate buildPredicate(AdminUserFilter filter) {
        QUser qUser = QUser.user;
        BooleanBuilder builder = new BooleanBuilder();

        if (filter.ids() != null && !filter.ids().isEmpty()) {
            builder.and(qUser.id.in(filter.ids()));
        }

        return builder.getValue();
    }

    private void emailExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Пользователь с почтой: " + email + " уже существует");
        }
    }
}
