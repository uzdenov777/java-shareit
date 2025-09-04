package ru.practicum.shareit.user.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * TODO Sprint add-controllers.
 */

@Data
public class User {
    Long id;

    @NotBlank(message = "name у пользователя не может отсутствовать")
    String name;

    @NotBlank(message = "Email у пользователя не может отсутствовать")
    @Email(message = "Email у пользователя не верного формата")
    String email;
}