package ru.practicum.shareit.booking.model.dto;

import lombok.Data;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;

@Data
public class BookingResponse {

    private Long id;

    private LocalDateTime start;

    private LocalDateTime end;

    private BookingStatus status;

    private UserResponse booker;

    private ItemResponse item;

    @Data
    public static class ItemResponse {

        private Long id;

        private String name;

        private String description;
    }

    @Data
    public static class UserResponse {

        private Long id;

        private String name;

        private String email;
    }
}