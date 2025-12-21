package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.booking.model.dto.BookingRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO Sprint add-bookings.
 */
@Slf4j
@RestController
@RequestMapping(path = "/bookings")
public class BookingController {
    private final BookingClient bookingClient;

    @Autowired
    public BookingController(BookingClient bookingClient) {
        this.bookingClient = bookingClient;
    }

    @PostMapping
    public ResponseEntity<Object> addBooking(@RequestHeader("X-Sharer-User-Id") Long bookerId, @RequestBody @Valid BookingRequest booking) {
        log.info("Запрос на добавление нового бронирования {}, пользователем по ID: {}", booking, bookerId);
        return bookingClient.add(bookerId, booking);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<Object> confirmingOrRejectingBookingRequest(@RequestHeader("X-Sharer-User-Id") Long userId,
                                                                      @PathVariable("bookingId") Long bookingId,
                                                                      @RequestParam @NotNull Boolean approved) {
        log.info("Запрос на подтверждение или отклонение запроса на бронирование по ID: {}, пользователем по ID: {}, решение: {}", bookingId, userId, approved);
        return bookingClient.confirmingOrRejectingBookingRequest(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Object> getBookingById(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable("bookingId") Long bookingId) throws ResponseStatusException {
        log.info("Запрос на возвращение бронирования по ID: {}, пользователем по ID: {}", bookingId, userId);
        return bookingClient.getBookingById(userId, bookingId);
    }

    @GetMapping
    public ResponseEntity<Object> getListAllBookingsForCurrentBooker(@RequestHeader("X-Sharer-User-Id") Long userId,
                                                                     @RequestParam(name = "state", defaultValue = "all") String state,
                                                                     @RequestParam(name = "from", defaultValue = "0") int from,
                                                                     @RequestParam(name = "size", defaultValue = "10") int size) {
        try {
            log.info("Запрос на возвращение всех бронирований со статусом {} текущего арендодателя по ID: {}", state, userId);

            BookingStateFilter bookingStateFilter = BookingStateFilter.valueOf(state.toUpperCase());//Пробуем привести к Enum, если что ловим ошибку

            ResponseEntity<Object> resBookings = bookingClient.getListAllBookingsForCurrentUser(userId, state, from, size);
            return resBookings;
        } catch (IllegalArgumentException e) {
            log.error("Был передан не существующий статус при запросе на возвращении всех бронирований со статусом {} текущего арендодателя по ID: {}", state, userId);

            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", "Unknown state: " + state);

            return ResponseEntity.internalServerError().body(errorBody);
        }
    }

    @GetMapping("/owner")
    public ResponseEntity<Object> getListBookingsForCurrentOwner(@RequestHeader("X-Sharer-User-Id") Long userId,
                                                                 @RequestParam(name = "state", defaultValue = "all") String state,
                                                                 @RequestParam(name = "from", defaultValue = "0") int from,
                                                                 @RequestParam(name = "size", defaultValue = "10") int size) {
        try {
            log.info("Запрос на возвращение всех бронирований со статусом {} текущего хозяина по ID: {}", state, userId);

            //Пробуем привести к Enum, если что ловим ошибку
            BookingStateFilter bookingStateFilter = BookingStateFilter.valueOf(state.toUpperCase());

            ResponseEntity<Object> resBookings = bookingClient.getListAllBookingsForCurrentOwner(userId, state, from, size);
            return resBookings;
        } catch (IllegalArgumentException e) {
            log.error("Был передан не существующий статус при запросе на возвращении всех бронирований со статусом {} текущего хозяина по ID: {}", state, userId);

            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", "Unknown state: " + state);
            return ResponseEntity.internalServerError().body(errorBody);
        }
    }
}