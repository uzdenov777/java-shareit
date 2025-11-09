package ru.practicum.shareit.item.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItemResponse {

    private Long id;

    private String name;

    private String description;

    private LocalDateTime lastBooking;

    private LocalDateTime nextBooking;

    private boolean available;

    private Long requestId;

    private List<CommentResponse> comments;
}
