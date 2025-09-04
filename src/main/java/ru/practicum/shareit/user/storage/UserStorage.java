package ru.practicum.shareit.user.storage;

import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.model.dto.UserDto;

public interface UserStorage {

    User add(User user);

    User update(Long userId, UserDto userDto);

    User delete(Long userId);

    User getUser(Long userId);

    boolean existsUser(Long userId);
}