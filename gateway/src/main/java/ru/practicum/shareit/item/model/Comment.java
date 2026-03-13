package ru.practicum.shareit.item.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;

@Data
public class Comment {

    private Long id;

    @NotNull
    private User author;

    @NotNull
    private Item item;

    @NotBlank
    private String text;

    @NotNull
    private LocalDateTime created;
}