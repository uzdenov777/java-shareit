package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.model.dto.UserDto;
import ru.practicum.shareit.user.storage.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    private UserDto userDto;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    public void setUp() {
        String name = "John";
        String email = "email@gmail.com";

        userDto = new UserDto();
        userDto.setName(name);
        userDto.setEmail(email);
    }

    @Test
    void addUser_whenInputUserValid_thenSaveUser() {
        //given
        String name = userDto.getName();
        String email = userDto.getEmail();

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        {
                            User save = invocation.getArgument(0);
                            save.setId(1L);
                            return save;
                        }
                );

        //when
        UserDto res = userService.addUser(userDto);

        //then
        Long idResUser = res.getId();
        String nameResUser = res.getName();
        String emailResUser = res.getEmail();

        assertEquals(1L, idResUser);
        assertEquals(name, nameResUser);
        assertEquals(email, emailResUser);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void addUser_whenUserEmailBusy_thenThrowResponseStatusException() {
        //given
        String email = userDto.getEmail();

        DataIntegrityViolationException exception = new DataIntegrityViolationException(email);

        when(userRepository.save(any(User.class))).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userService.addUser(userDto));

        //then
        String exceptionMessage = "409 CONFLICT \"Электронная почта уже занята email: " + email + "\"";
        HttpStatus exceptionStatus = HttpStatus.CONFLICT;

        assertEquals(exceptionMessage, resException.getMessage());
        assertEquals(exceptionStatus, resException.getStatusCode());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_whenFoundUserForUpdate_thenUpdateUser() {
        //given
        Long userId = 1L;

        User foundUserForUpdate = userService.toUser(userDto);
        foundUserForUpdate.setId(userId);

        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setName("updatedName");
        updatedUserDto.setEmail("updatedEmail@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(foundUserForUpdate));
        when(userRepository.save(any(User.class))).thenReturn(foundUserForUpdate);

        //when
        User resUser = userService.updateUser(userId, updatedUserDto);

        //then
        Long idResUser = resUser.getId();
        String nameResUser = resUser.getName();
        String emailResUser = resUser.getEmail();

        assertEquals(1L, idResUser);
        assertEquals(updatedUserDto.getName(), nameResUser);
        assertEquals(updatedUserDto.getEmail(), emailResUser);
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_whenNotFoundUserForUpdate_thenThrowResponseStatusException() {
        //given
        Long notExistingUserId = 7777L;
        HttpStatus exceptionStatus = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найден пользователь для обновления с ID: " + notExistingUserId + "\"";

        when(userRepository.findById(notExistingUserId)).thenReturn(Optional.empty());

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userService.updateUser(notExistingUserId, userDto));

        //then
        assertEquals(exceptionStatus, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userRepository).findById(notExistingUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_whenUserFieldsNullOrEmpty_thenThrowResponseStatusException() {
        //given
        Long userId = 1L;

        User foundUserForUpdate = userService.toUser(userDto);
        foundUserForUpdate.setId(userId);

        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setName("");
        updatedUserDto.setEmail(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(foundUserForUpdate));

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userService.updateUser(userId, updatedUserDto));

        //then
        HttpStatus exceptionStatus = HttpStatus.CONFLICT;
        String exceptionMessage = "409 CONFLICT \"Все новые поля пустые для обновления пользователя по ID: " + userId + "\"";

        assertEquals(exceptionStatus, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_whenUserEmailBusy_thenThrowResponseStatusException() {
        //given
        Long userId = 1L;
        String emailBusy = "busyEmail@gmail.com";

        User foundUserForUpdate = userService.toUser(userDto);
        foundUserForUpdate.setId(userId);

        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setName("updatedName");
        updatedUserDto.setEmail(emailBusy);

        DataIntegrityViolationException exception = new DataIntegrityViolationException(emailBusy);

        when(userRepository.findById(userId)).thenReturn(Optional.of(foundUserForUpdate));
        when(userRepository.save(any(User.class))).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userService.updateUser(userId, updatedUserDto));

        //then
        HttpStatus exceptionStatus = HttpStatus.CONFLICT;
        String exceptionMessage = "409 CONFLICT \"При обновлении пользователя обнаружено что, электронная почта уже занята email: " + emailBusy + "\"";

        assertEquals(exceptionStatus, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void removeUser_whenUserFound_thenRemoveUser() {
        //given
        Long userId = 1L;

        User foundUserForDelete = userService.toUser(userDto);
        foundUserForDelete.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(foundUserForDelete));

        //when
        User remoteUser = userService.removeUser(userId);

        //then
        assertEquals(foundUserForDelete, remoteUser);
        verify(userRepository).findById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void removeUser_whenUserNotFound_thenThrowResponseStatusException() {
        //given
        Long notExsUserId = 777L;

        when(userRepository.findById(notExsUserId)).thenReturn(Optional.empty());

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userService.removeUser(notExsUserId));

        //then
        HttpStatus exceptionStatus = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найден пользователь для удаления с ID: " + notExsUserId + "\"";

        assertEquals(exceptionStatus, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userRepository).findById(notExsUserId);
        verify(userRepository, never()).deleteById(notExsUserId);
    }

    @Test
    void getUserById_whenUserFound_thenGetUserById() {
        //given
        Long userId = 1L;

        User foundUserForRet = userService.toUser(userDto);
        foundUserForRet.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(foundUserForRet));

        //when
        User userRes = userService.getUserById(userId);

        //then
        assertEquals(foundUserForRet, userRes);
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_whenUserNotFound_thenThrowResponseStatusException() {
        //given
        Long userId = 777L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> userService.getUserById(userId));

        //then
        HttpStatus exceptionStatus = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найден пользователь для возвращения с ID: " + 777 + "\"";

        assertEquals(exceptionStatus, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userRepository).findById(userId);
    }

    @Test
    void getAllUsers_whenUSersExist_thenGetAllUsers() {
        //given
        List<User> users = List.of(new User(), new User(), new User());

        when(userRepository.findAll()).thenReturn(users);

        //when
        List<User> usersRes = userService.getAllUsers();

        //then
        assertEquals(users, usersRes);
    }

    @Test
    void getAllUsers_whenUserNotFound_thenReturnEmptyList() {
        //given
        List<User> users = List.of();

        when(userRepository.findAll()).thenReturn(users);
        //when
        List<User> usersRes = userService.getAllUsers();

        //then
        assertEquals(users, usersRes);
    }

    @Test
    void existsUser_whenUserExist_thenTrue() {
        //given
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);

        //when
        Boolean res = userService.existsUser(userId);

        //then
        assertEquals(true, res);
    }

    @Test
    void existsUser_whenUserNotExist_thenFalse() {
        //given
        Long userId = 777L;

        when(userRepository.existsById(userId)).thenReturn(false);

        //when
        Boolean res = userService.existsUser(userId);

        //then
        assertEquals(false, res);
    }
}