package ru.practicum.shareit.item.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {

    @NotBlank(message = "Текст комментария не должен быть пустым")
    String text;
}