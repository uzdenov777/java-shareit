package ru.practicum.shareit.item.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * TODO Sprint add-controllers.
 */

@Data
public class ItemDto {

    private Long id;

    @NotBlank(message = "Название  у вещи не может отсутствовать")
    private String name;

    @NotBlank(message = "Описание у вещи не может отсутствовать")
    private String description;

    @NotNull(message = "Не может отсутствовать статус у Вещи")
    private Boolean available;

    private Long requestId;
}