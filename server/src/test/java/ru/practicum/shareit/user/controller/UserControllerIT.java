package ru.practicum.shareit.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.model.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerIT {

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    private UserDto userDto;

    private User userEntity;

    private final Long userId = 1L;
    private final String nameUser = "nameDto";
    private final String emailUser = "dto@email.com";

    @BeforeEach
    public void setUp() {
        userDto = new UserDto();
        userDto.setName(nameUser);
        userDto.setEmail(emailUser);

        userEntity = new User();
        userEntity.setName(nameUser);
        userEntity.setEmail(emailUser);
    }

    @SneakyThrows
    @Test
    void saveUser_whenUserDtoIsValid_thenReturnAndSavedUserDto() {
        //given
        when(userService.addUser(any(UserDto.class)))
                .thenAnswer(i -> {

                    userDto.setId(userId);

                    return userDto;
                });

        //when+then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value(nameUser))
                .andExpect(jsonPath("$.email").value(emailUser));
    }

    @SneakyThrows
    @Test
    void saveUser_whenUserDtoFieldsEmpty_thenReturnStatusOk() {
        //given
        //поля пустые
        UserDto userDtoNew = new UserDto();

        //when+then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDtoNew)))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void saveUser_whenEmailNotValid_thenReturnBadRequest() {
        //given
        //Невалидный email
        userDto.setEmail("notValidEmail");

        //when+then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void saveUser_whenEmptyBody_thenReturnBadRequest() {
        //when+then
        //тело запроса пустое
        mockMvc.perform(post("/users"))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void updateUser_whenUserDtoIsValid_thenReturnAndUpdatedUserEntity() {
        //given
        when(userService.updateUser(anyLong(), any(UserDto.class)))
                .thenAnswer(i -> {

                    userEntity.setId(userId);

                    return userEntity;
                });

        //when+then
        mockMvc.perform(patch("/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value(nameUser))
                .andExpect(jsonPath("$.email").value(emailUser));
    }

    @SneakyThrows
    @Test
    void updateUser_whenUserIdNotNumber_thenReturnBadRequest() {
        //given
        // userId не цифра
        String userId = "notNumber";

        //when+then
        mockMvc.perform(patch("/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void updateUser_whenUserDtoFieldsEmpty_thenReturnStatusOk() {
        //given
        //поля пустые
        UserDto userDtoNew = new UserDto();

        //when+then
        mockMvc.perform(patch("/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDtoNew)))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void updateUser_whenEmailNotValid_thenReturnBadRequest() {
        //given
        //Невалидный email
        userDto.setEmail("notValidEmail");

        //when+then
        mockMvc.perform(patch("/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void updateUser_whenEmptyBody_thenReturnBadRequest() {
        //when+then
        //тело запроса пустое
        mockMvc.perform(patch("/users/{userId}", userId))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void deleteUser_whenRequestIsValid_thenReturnAndDeleteUserEntity() {
        //given
        when(userService.removeUser(userId))
                .thenAnswer(i -> {

                    userEntity.setId(userId);

                    return userEntity;
                });

        //when+then
        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value(nameUser))
                .andExpect(jsonPath("$.email").value(emailUser));
    }

    @SneakyThrows
    @Test
    void deleteUser_whenUserIdNotNumber_thenReturnBadRequest() {
        //given
        // userId не цифра
        String userId = "notNumber";

        //when+then
        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById() {
    }

    @SneakyThrows
    @Test
    void getUserById_whenRequestIsValid_thenReturnUserEntity() {
        //given
        when(userService.getUserById(userId))
                .thenAnswer(i -> {

                    userEntity.setId(userId);

                    return userEntity;
                });

        //when+then
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value(nameUser))
                .andExpect(jsonPath("$.email").value(emailUser));
    }

    @SneakyThrows
    @Test
    void getUserById_whenUserIdNotNumber_thenReturnBadRequest() {
        //given
        // userId не цифра
        String userId = "notNumber";

        //when+then
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void getUsers() {
        //given
        userEntity.setId(1L);
        when(userService.getAllUsers()).thenReturn(List.of(userEntity));

        //when+then
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(userId))
                .andExpect(jsonPath("$[0].name").value(nameUser))
                .andExpect(jsonPath("$[0].email").value(emailUser));
    }
}