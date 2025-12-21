package ru.practicum.shareit.user;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.user.model.dto.UserDto;

import java.util.HashMap;

/**
 * TODO Sprint add-controllers.
 */
@Slf4j
@RestController
@RequestMapping(path = "/users")

public class UserController {
    private final UserClient userClient;

    @Autowired
    public UserController(UserClient userClient) {
        this.userClient = userClient;
    }

    @PostMapping
    public ResponseEntity<Object> saveUser(@RequestBody @Valid UserDto userDto) {
        try {
            log.info("Сохранение пользователя {}", userDto);
            return userClient.addUser(userDto);
        } catch (ConstraintViolationException e) {
            HashMap<String, String> error = new HashMap<>();
            String errorMessage = e.getMessage();
            error.put("error", errorMessage);

            return ResponseEntity.badRequest().body(error);
        }
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<Object> updateUser(@PathVariable @Positive Long userId, @RequestBody @Valid UserDto userDto) {
        log.info("Updating user ID:{}", userId);
        return userClient.updateUser(userId, userDto);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Object> deleteUser(@PathVariable Long userId) {
        log.info("Deleting user {}", userId);
        return userClient.removeUser(userId);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Object> getUserById(@PathVariable Long userId) {
        log.info("Getting user {}", userId);
        return userClient.getUserById(userId);
    }

    @GetMapping
    public ResponseEntity<Object> getUsers() {
        log.info("Getting All users");
        return userClient.getAllUsers();
    }
}