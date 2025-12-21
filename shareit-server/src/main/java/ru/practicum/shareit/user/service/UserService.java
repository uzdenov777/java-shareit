package ru.practicum.shareit.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.model.dto.UserDto;
import ru.practicum.shareit.user.storage.UserRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userStorage) {
        this.userRepository = userStorage;
    }

    public UserDto addUser(UserDto userDto) {
        try {
            User newUser = toUser(userDto);

            User save = userRepository.save(newUser);

            UserDto savedUserDto = toUserDto(save);
            return savedUserDto;

        } catch (DataIntegrityViolationException e) {
            String email = userDto.getEmail();
            log.info("Электронная почта уже занята email: {}", email);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Электронная почта уже занята email: " + email);
        }
    }

    public User updateUser(Long userId, UserDto userDto) throws ResponseStatusException {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            log.info("Не найден пользователь для обновления с ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь для обновления с ID: " + userId);
        }

        User existingUser = userOpt.get();

        String nameDto = userDto.getName();
        String emailDto = userDto.getEmail();

        boolean isNotEmptyName = StringUtils.hasText(nameDto);
        boolean isNotEmptyEmail = StringUtils.hasText(emailDto);

        if (isNotEmptyName && isNotEmptyEmail) {
            existingUser.setName(nameDto);
            existingUser.setEmail(emailDto);

        } else if (isNotEmptyName) {
            existingUser.setName(nameDto);

        } else if (isNotEmptyEmail) {
            existingUser.setEmail(emailDto);

        } else {
            log.info("Все новые поля пустые для обновления пользователя по ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Все новые поля пустые для обновления пользователя по ID: " + userId);
        }

        try {
            User updatedUser = userRepository.save(existingUser);
            return updatedUser;

        } catch (DataIntegrityViolationException e) { // Может выбросить исключение из-за того новая почта уже есть в бд
            log.info("При обновлении пользователя обнаружено что, электронная почта уже занята email: {}", emailDto);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "При обновлении пользователя обнаружено что, электронная почта уже занята email: " + emailDto);
        }
    }

    public User removeUser(Long userId) throws ResponseStatusException {

        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            log.info("Не найден пользователь для удаления с ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь для удаления с ID: " + userId);
        }

        userRepository.deleteById(userId);

        User remoteUser = userOpt.get();
        return remoteUser;
    }

    public User getUserById(Long userId) throws ResponseStatusException {

        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            log.info("Не найден пользователь для возвращения с ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь для возвращения с ID: " + userId);
        }

        User user = userOpt.get();
        return user;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean existsUser(Long userId) {
        return userRepository.existsById(userId);
    }

    private User toUser(UserDto userDto) {

        String name = userDto.getName();
        String email = userDto.getEmail();

        User user = new User();

        user.setName(name);
        user.setEmail(email);

        return user;
    }

    private UserDto toUserDto(User save) {
        Long id = save.getId();
        String name = save.getName();
        String email = save.getEmail();

        UserDto userDto = new UserDto();

        userDto.setId(id);
        userDto.setName(name);
        userDto.setEmail(email);

        return userDto;
    }
}