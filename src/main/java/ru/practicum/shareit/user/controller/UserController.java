package ru.practicum.shareit.user.controller;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.model.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.HashMap;
import java.util.List;

/**
 * TODO Sprint add-controllers.
 */
@Slf4j
@RestController
@RequestMapping(path = "/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> saveUser(@RequestBody @Valid UserDto userDto) {
        try {
            log.info("Сохранение пользователя {}", userDto);
            return ResponseEntity.ok(userService.addUser(userDto));
        }catch (ConstraintViolationException e){
            HashMap<String, String> error = new HashMap<>();
            String errorMessage = e.getMessage();
            error.put("error", errorMessage);

            return ResponseEntity.badRequest().body(error);
        }
    }

    @PatchMapping("/{userId}")
    public User updateUser(@PathVariable Long userId, @RequestBody @Valid UserDto userDto) {
        log.info("Updating user ID:{}", userId);
        return userService.updateUser(userId, userDto);
    }

    @DeleteMapping("/{userId}")
    public User deleteUser(@PathVariable Long userId) {
        log.info("Deleting user {}", userId);
        return userService.removeUser(userId);
    }

    @GetMapping("/{userId}")
    public User getUserById(@PathVariable Long userId) {
        log.info("Getting user {}", userId);
        return userService.getUserById(userId);
    }

    @GetMapping
    public List<User> getUsers() {
        log.info("Getting All users");
        return userService.getAllUsers();
    }
}