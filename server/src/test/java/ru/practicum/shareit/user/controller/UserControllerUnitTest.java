package ru.practicum.shareit.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.model.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

    private UserDto userDto;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        String name = "John";
        String email = "email@gmail.com";

        userDto = new UserDto();
        userDto.setName(name);
        userDto.setEmail(email);
    }

    @Test
    void saveUser_whenUserValid_thenSaveUser() {
        //given
        when(userService.addUser(userDto)).thenReturn(userDto);

        //when
        ResponseEntity<Object> res = userController.saveUser(userDto);

        //then
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(userDto, res.getBody());
        verify(userService).addUser(userDto);
    }

    @Test
    void saveUser_whenUserEmailBusy_thenThrowResponseStatusException() {
        //given
        String email = userDto.getEmail();

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.CONFLICT, "Электронная почта уже занята email: " + email);

        when(userService.addUser(userDto)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userController.saveUser(userDto));

        //then
        assertEquals(exception, resException);
        verify(userService).addUser(userDto);
    }

    @Test
    void updateUser_whenUserValid_thenUpdateUser() {
        //given
        String name = "NewVersionUser";
        String email = "email@gmail.com";
        Long userId = 1L;

        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setName(name);
        updatedUserDto.setEmail(email);

        User updatedUser = new User(userId, name, email); //это updatedUserDto только toUser

        when(userService.updateUser(userId, updatedUserDto)).thenReturn(updatedUser);

        //when
        User resUser = userController.updateUser(userId, updatedUserDto);

        //then
        assertEquals(updatedUser, resUser);
        verify(userService).updateUser(userId, updatedUserDto);
    }

    @Test
    void updateUser_whenUserNotFound_thenThrowResponseStatusExceptionStatusNotFound() {
        //given
        String name = "NewVersionUser";
        String email = "email@.com";
        Long notExistUserId = 99999L;

        UserDto updateUserDto = new UserDto();
        updateUserDto.setName(name);
        updateUserDto.setEmail(email);

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь для обновления с ID: " + notExistUserId);

        when(userService.updateUser(notExistUserId, updateUserDto)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userController.updateUser(notExistUserId, updateUserDto));

        //then
        assertEquals(exception, resException);
        verify(userService).updateUser(notExistUserId, updateUserDto);
    }

    @Test
    void updateUser_whenUserEmptyNewFields_thenThrowResponseStatusExceptionStatusConflict() {
        //given
        String name = "";
        String email = "";
        Long notExistUserId = 99999L;

        UserDto updateUserDto = new UserDto();
        updateUserDto.setName(name);
        updateUserDto.setEmail(email);

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.CONFLICT, "Все новые поля пустые для обновления пользователя по ID: " + notExistUserId);

        when(userService.updateUser(notExistUserId, updateUserDto)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userController.updateUser(notExistUserId, updateUserDto));

        //then
        assertEquals(exception, resException);
        verify(userService).updateUser(notExistUserId, updateUserDto);
    }

    @Test
    void updateUser_whenUserNewEmailBusy_thenThrowResponseStatusExceptionStatusNotConflict() {
        //given
        String name = "NewVersionUser";
        String emailBusy = "email@.com";
        Long notExistUserId = 99999L;

        UserDto updateUserDto = new UserDto();
        updateUserDto.setName(name);
        updateUserDto.setEmail(emailBusy);

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "При обновлении пользователя обнаружено что, электронная почта уже занята email: " + notExistUserId);

        when(userService.updateUser(notExistUserId, updateUserDto)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userController.updateUser(notExistUserId, updateUserDto));

        //then
        assertEquals(exception, resException);
        verify(userService).updateUser(notExistUserId, updateUserDto);
    }

    @Test
    void deleteUser_whenUserExist_thenDeleteUser() {
        //given
        Long userId = 1L;

        User deletedUser = new User(userId, "deletedUser", "deletedEmail@.com");

        when(userService.removeUser(userId)).thenReturn(deletedUser);

        //when
        User res = userController.deleteUser(userId);

        //then
        assertEquals(deletedUser, res);
        verify(userService).removeUser(userId);
    }

    @Test
    void deleteUser_whenUserNotExist_thenThrowResponseStatusExceptionStatusNotFound() {
        //given
        Long notExistUserId = 9999L;
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь для удаления с ID: " + notExistUserId);
        when(userService.removeUser(notExistUserId)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(notExistUserId));

        //then
        assertEquals(exception, resException);
        verify(userService).removeUser(notExistUserId);
    }

    @Test
    void getUserById_whenUserExist_thenGetUser() {
        //given
        Long userId = 1L;
        User gettingUser = new User(userId, "gettingUser", "gettingEmail@.com");
        when(userService.getUserById(userId)).thenReturn(gettingUser);

        //when
        User res = userController.getUserById(userId);

        //then
        assertEquals(gettingUser, res);
        verify(userService).getUserById(userId);
    }

    @Test
    void getUserById_whenUserNotExist_thenThrowResponseStatusExceptionStatusNotFound() {
        //given
        Long notExistUserId = 9999L;
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь для возвращения с ID: " + notExistUserId);
        when(userService.getUserById(notExistUserId)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userController.getUserById(notExistUserId));

        //then
        assertEquals(exception, resException);
        verify(userService).getUserById(notExistUserId);
    }

    @Test
    void getUsers_whenUsersExist_thenGetListAllUsers() {
        //given
        List<User> users = List.of(new User(1L, "gettingUser", "gettingEmail@.com"), new User(2L, "gettingUser2", "gettingEmail2@.com"));
        when(userService.getAllUsers()).thenReturn(users);

        //when
        List<User> res = userController.getUsers();

        //then
        assertEquals(users, res);
        assertFalse(res.isEmpty());
        verify(userService).getAllUsers();
    }

    @Test
    void getUsers_whenUsersNotExist_thenGetEmptyList() {
        //given
        List<User> users = List.of();
        when(userService.getAllUsers()).thenReturn(users);

        //when
        List<User> res = userController.getUsers();

        //then
        assertEquals(users, res);
        assertTrue(res.isEmpty());
        verify(userService).getAllUsers();
    }
}