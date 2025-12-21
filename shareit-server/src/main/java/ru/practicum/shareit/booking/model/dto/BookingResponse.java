package ru.practicum.shareit.booking.model.dto;

import lombok.Data;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;

@Data
public class
BookingResponse {

    private Long id;

    private LocalDateTime start;

    private LocalDateTime end;

    private BookingStatus status;

    private User booker;

    private ItemRes item;

    @Data
    public static class ItemRes {

        private Long id;

        private String name;

        private String description;
    }
}