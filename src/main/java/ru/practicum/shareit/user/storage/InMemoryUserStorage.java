package ru.practicum.shareit.user.storage;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.model.dto.UserDto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Repository
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> userStorage = new HashMap<>();
    private final Set<String> userEmails = new HashSet<>();
    private static Long newId = 0L;

    @Override
    public User add(User user) {

        Long userId = getNewId();

        user.setId(userId);

        userStorage.put(userId, user);
        userEmails.add(user.getEmail());

        return user;
    }

    @Override
    public User update(Long userId, UserDto userDto) {

        User userToUpdate = userStorage.get(userId);
        userToUpdate.setName(userDto.getName());
        userToUpdate.setEmail(userDto.getEmail());

        return userToUpdate;
    }

    public User updateNameUser(Long userId, UserDto userDto) {

        User userToUpdate = userStorage.get(userId);
        userToUpdate.setName(userDto.getName());

        return userToUpdate;
    }

    public User updateEmailUser(Long userId, UserDto userDto) {

        User userToUpdate = userStorage.get(userId);
        userToUpdate.setEmail(userDto.getEmail());

        return userToUpdate;
    }

    @Override
    public User delete(Long userId) {
        return userStorage.remove(userId);
    }

    @Override
    public User getUser(Long userId) {
        return userStorage.get(userId);
    }

    @Override
    public boolean existsUser(Long userId) {
        return userStorage.containsKey(userId);
    }

    public boolean existsEmail(String email) {
        return userEmails.contains(email);
    }

    private Long getNewId() {
        return ++newId;
    }
}