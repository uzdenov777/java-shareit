package ru.practicum.shareit.booking.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.booking.model.dto.BookingRequest;
import ru.practicum.shareit.booking.model.dto.BookingResponse;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.booking.service.BookingStateFilter;
import java.util.List;

/**
 * TODO Sprint add-bookings.
 */
@Slf4j
@RestController
@RequestMapping(path = "/bookings")
public class BookingController {
    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponse addBooking(@RequestHeader("X-Sharer-User-Id") Long bookerId, @RequestBody @Valid BookingRequest booking) {
        log.info("Запрос на добавление нового бронирования {}, пользователем по ID: {}", booking, bookerId);
        return bookingService.add(bookerId, booking);
    }

    @PatchMapping("/{bookingId}")
    public BookingResponse confirmingOrRejectingBookingRequest(@RequestHeader("X-Sharer-User-Id") Long userId,
                                                               @PathVariable("bookingId") Long bookingId,
                                                               @RequestParam @NotNull Boolean approved) {
        log.info("Запрос на подтверждение или отклонение запроса на бронирование по ID: {}, пользователем по ID: {}, решение: {}", bookingId, userId, approved);
        return bookingService.confirmingOrRejectingBookingRequest(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBookingById(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable("bookingId") Long bookingId) throws ResponseStatusException {
        log.info("Запрос на возвращение бронирования по ID: {}, пользователем по ID: {}", bookingId, userId);
        return bookingService.getBookingById(userId, bookingId);
    }

    @GetMapping
    public List<BookingResponse> getListAllBookingsForCurrentBooker(@RequestHeader("X-Sharer-User-Id") Long userId,
                                                                    @RequestParam(name = "state", defaultValue = "all") String state,
                                                                    @RequestParam(name = "from", defaultValue = "0") int from,
                                                                    @RequestParam(name = "size", defaultValue = "10") int size) {
        BookingStateFilter bookingStateFilter = BookingStateFilter.valueOf(state.toUpperCase());
        log.info("Запрос на возвращение всех бронирований со статусом {} текущего арендодателя по ID: {}", bookingStateFilter, userId);
        return bookingService.getListAllBookingsForCurrentUser(userId, bookingStateFilter, from, size);
    }

    @GetMapping("/owner")
    public List<BookingResponse> getListBookingsForCurrentOwner(@RequestHeader("X-Sharer-User-Id") Long userId,
                                                                @RequestParam(name = "state", defaultValue = "all") String state,
                                                                @RequestParam(name = "from", defaultValue = "0") int from,
                                                                @RequestParam(name = "size", defaultValue = "10") int size) {
        BookingStateFilter bookingStateFilter = BookingStateFilter.valueOf(state.toUpperCase());
        log.info("Запрос на возвращение всех бронирований со статусом {} текущего хозяина по ID: {}", bookingStateFilter, userId);
        return bookingService.getListAllBookingsForCurrentOwner(userId, bookingStateFilter, from, size);
    }
}