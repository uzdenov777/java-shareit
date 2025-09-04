package ru.practicum.shareit.item.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * TODO Sprint add-controllers.
 */

@Data
public class Item {

    private Long id;

    @NotBlank(message = "Название  у вещи не может отсутствовать")
    private String name;

    @NotBlank(message = "Описание у вещи не может отсутствовать")
    private String description;

    @NotNull(message = "Не может отсутствовать статус у Вещи")
    private Boolean available;

    private Long idOwner;
}