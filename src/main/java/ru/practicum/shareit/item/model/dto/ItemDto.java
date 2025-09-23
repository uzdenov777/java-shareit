package ru.practicum.shareit.item.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TODO Sprint add-controllers.
 */

@Data
public class ItemDto {

    private Long id;

    private String name;

    private String description;

    private LocalDateTime lastBooking;

    private LocalDateTime nextBooking;

    private boolean available;

    private List<CommentResponse> comments;
}