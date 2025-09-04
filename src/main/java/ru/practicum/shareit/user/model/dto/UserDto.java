package ru.practicum.shareit.user.model.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserDto {
    String name;

    @Email(message = "Email should be valid")
    String email;
}