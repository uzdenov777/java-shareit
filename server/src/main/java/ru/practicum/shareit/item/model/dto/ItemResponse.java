package ru.practicum.shareit.item.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ItemResponse {

    private Long id;

    private String name;

    private String description;

    private BookingRes lastBooking;

    private BookingRes nextBooking;

    private boolean available;

    private Long requestId;

    private List<CommentResponse> comments;

    @Data
    public static class BookingRes {

        private Long id;

        private Long bookerId;
    }
}