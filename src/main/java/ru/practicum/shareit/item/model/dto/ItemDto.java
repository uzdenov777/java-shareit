package ru.practicum.shareit.item.model.dto;

import lombok.Data;

/**
 * TODO Sprint add-controllers.
 */

@Data
public class ItemDto {

    private Long id;

    private String name;

    private String description;

    private boolean available;
}
