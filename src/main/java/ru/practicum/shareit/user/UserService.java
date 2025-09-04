package ru.practicum.shareit.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.model.dto.UserDto;
import ru.practicum.shareit.user.storage.InMemoryUserStorage;

import java.util.Objects;

@Slf4j
@Service
public class UserService {
    private final InMemoryUserStorage userStorage;

    public UserService(InMemoryUserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User addUser(User user) {
        String email = user.getEmail();

        isBusyEmail(email); //выбросит исключение если электронная почта уже используется

        return userStorage.add(user);
    }

    public User updateUser(Long userId, UserDto userDto) throws ResponseStatusException {
        boolean exists = userStorage.existsUser(userId);
        if (!exists) {
            log.info("Не найден пользователь для обновления с ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь для обновления с ID: " + userId);
        }

        if (Objects.isNull(userDto.getName())) {
            userDto.setName("");
        }

        if (Objects.isNull(userDto.getEmail())) {
            userDto.setEmail("");
        }

        String name = userDto.getName();
        String email = userDto.getEmail();

        if (!name.isBlank() && !email.isBlank()) {
            isBusyEmail(email);

            return userStorage.update(userId, userDto);

        } else if (!name.isBlank()) {
            return userStorage.updateNameUser(userId, userDto);

        } else if (!email.isBlank()) {
            isBusyEmail(email);

            return userStorage.updateEmailUser(userId, userDto);

        } else {
            log.info("Все новые поля пустые для обновления пользователя по ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Все новые поля пустые для обновления пользователя по ID: " + userId);
        }
    }

    public User removeUser(Long userId) throws ResponseStatusException {

        boolean exists = userStorage.existsUser(userId);

        if (!exists) {
            log.info("Не найден пользователь для удаления с ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь для удаления с ID: " + userId);
        }

        return userStorage.delete(userId);
    }

    public User getUserById(Long userId) throws ResponseStatusException {

        boolean exists = userStorage.existsUser(userId);
        if (!exists) {
            log.info("Не найден пользователь для возвращения с ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь для возвращения с ID: " + userId);
        }

        return userStorage.getUser(userId);
    }

    private void isBusyEmail(String email) throws ResponseStatusException {
        boolean isBusyEmail = userStorage.existsEmail(email);
        if (isBusyEmail) {
            log.info("Электронная почта уже занята email: {}", email);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Электронная почта уже занята email: " + email);
        }
    }

    public boolean isExistsUser(Long userId) {
        return userStorage.existsUser(userId);
    }
}