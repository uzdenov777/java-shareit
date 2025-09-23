package ru.practicum.shareit.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.model.dto.BookingRequest;
import ru.practicum.shareit.booking.model.dto.BookingResponse;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ItemService itemService;

    @Autowired
    public BookingService(BookingRepository bookingRepository, UserService userService, ItemService itemService) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.itemService = itemService;

    }

    public BookingResponse add(Long bookerId, BookingRequest bookingRequest) throws ResponseStatusException {

        Booking bookingEntity = toBooking(bookerId, bookingRequest);

        checkPossibilityBooking(bookingEntity);

        Booking saveBookingEntity = bookingRepository.save(bookingEntity);

        BookingResponse bookingResponse = toBookingResponse(saveBookingEntity);

        return bookingResponse;
    }

    public BookingResponse confirmingOrRejectingBookingRequest(Long userId, Long bookingId, Boolean solution) {
        Booking booking = getBooking(bookingId);
        Item item = booking.getItem();
        User owner = item.getOwner();
        Long ownerId = owner.getId();

        if (!userId.equals(ownerId)) {
            log.info("Пользователь по ID: {} не может изменить статус бронирования по ID: {}, потому что не является владельцем вещи", userId, bookingId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь по ID: " + userId + " не может изменить статус бронирования по ID: " + bookingId + ", потому что не является владельцем вещи");
        }

        if (solution) {
            booking.setStatus(BookingStatus.APPROVED);
        } else {
            booking.setStatus(BookingStatus.REJECTED);
        }

        Booking saveBookingEntity = bookingRepository.save(booking);

        BookingResponse bookingResponse = toBookingResponse(saveBookingEntity);

        return bookingResponse;
    }

    public BookingResponse getBookingById(Long userId, Long bookingId) throws ResponseStatusException {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            log.info("При запросе на возвращение бронирование не найдено по ID: {}", bookingId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "При запросе на возвращение бронирование не найдено по ID: " + bookingId);
        }

        Booking booking = bookingOpt.get();

        boolean isOwnerOrBooker = isOwnerOrBooker(booking, userId);
        if (!isOwnerOrBooker) {
            log.info("Запрос на возвращение бронирования по ID: {} может только хозяин вещи, либо автор бронирования", bookingId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Запрос на возвращение бронирования по ID: " + bookingId + " может только хозяин вещи, либо автор бронирования");
        }

        BookingResponse bookingResponse = toBookingResponse(booking);
        return bookingResponse;
    }

    private Booking getBooking(Long bookingId) throws ResponseStatusException {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            log.info("При запросе на возвращение бронирование не найдено по ID: {} для изменения статуса", bookingId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "При запросе на возвращение бронирование не найдено по ID: " + bookingId + "для изменения статуса");
        }

        Booking booking = bookingOpt.get();

        return booking;
    }

    private boolean isOwnerOrBooker(Booking booking, Long userId) {

        Item item = booking.getItem();
        User owner = item.getOwner();
        User booker = booking.getBooker();

        Long ownerId = owner.getId();
        Long bookerId = booker.getId();

        boolean ownerOrBooker = (ownerId.equals(userId)) || (bookerId.equals(userId));

        return ownerOrBooker;
    }

    private Booking toBooking(Long bookerId, BookingRequest bookingRequest) {

        Long itemId = bookingRequest.getItemId();

        Item item = itemService.getItemById(itemId);
        User user = userService.getUserById(bookerId);
        LocalDateTime start = bookingRequest.getStart();
        LocalDateTime end = bookingRequest.getEnd();
        BookingStatus status = BookingStatus.WAITING;

        Booking bookingEntity = new Booking();
        bookingEntity.setItem(item);
        bookingEntity.setBooker(user);
        bookingEntity.setStart(start);
        bookingEntity.setEnd(end);
        bookingEntity.setStatus(status);

        return bookingEntity;
    }

    private BookingResponse toBookingResponse(Booking booking) {
        Long id = booking.getId();
        LocalDateTime start = booking.getStart();
        LocalDateTime end = booking.getEnd();
        BookingStatus status = booking.getStatus();

        Item item = booking.getItem();
        BookingResponse.ItemResponse itemResponse = toItemResponse(item);

        User user = booking.getBooker();
        BookingResponse.UserResponse userResponse = toUserResponse(user);

        BookingResponse bookingResponse = new BookingResponse();
        bookingResponse.setId(id);
        bookingResponse.setStart(start);
        bookingResponse.setEnd(end);
        bookingResponse.setStatus(status);
        bookingResponse.setItem(itemResponse);
        bookingResponse.setBooker(userResponse);

        return bookingResponse;
    }

    private BookingResponse.ItemResponse toItemResponse(Item item) {
        Long itemId = item.getId();
        String itemName = item.getName();
        String itemDescription = item.getDescription();

        BookingResponse.ItemResponse itemResponse = new BookingResponse.ItemResponse();
        itemResponse.setId(itemId);
        itemResponse.setName(itemName);
        itemResponse.setDescription(itemDescription);

        return itemResponse;
    }

    private BookingResponse.UserResponse toUserResponse(User user) {
        Long userId = user.getId();
        String name = user.getName();
        String email = user.getEmail();

        BookingResponse.UserResponse userResponse = new BookingResponse.UserResponse();
        userResponse.setId(userId);
        userResponse.setName(name);
        userResponse.setEmail(email);

        return userResponse;
    }

    private void checkPossibilityBooking(Booking booking) throws ResponseStatusException {
        Item bookingItem = booking.getItem();
        Long itemId = bookingItem.getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = booking.getStart();
        LocalDateTime end = booking.getEnd();

        boolean isEndAfterNow = end.isAfter(now);
        boolean isStartAfterNow = start.isAfter(now);
        boolean isEndEqualStart = start.isEqual(end);
        boolean isEndAfterStart = end.isAfter(start);
        boolean isAvailableItem = bookingItem.getAvailable();

        if (!isEndAfterNow) {
            log.info("Конец времени бронирования не может быть в прошлом! Конец: {}, для предмета по ID: {} ", end, itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Конец времени бронирования не может быть в прошлом! Конец: " + end + ", для предмета по ID: " + itemId);
        }

        if (!isStartAfterNow) {
            log.info("Начало времени бронирования не может быть в прошлом! Начало: {}, для предмета по ID: {} ", start, itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Начало времени бронирования не может быть в прошлом! Начало: " + start + ", для предмета по ID: " + itemId);
        }

        if (isEndEqualStart) {
            log.info("Конец времени бронирования не может быть равен началу! Начало: {}, конец: {}, для предмета по ID: {} ", start, end, itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Конец времени бронирования не может быть равен началу! Начало: " + start + ", конец: " + end + ", для предмета по ID: " + itemId);
        }

        if (!isEndAfterStart) {
            log.info("Конец времени бронирования не может быть раньше начала! Начало: {}, конец: {}, для предмета по ID: {} ", start, end, itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Конец времени бронирования не может быть раньше начала! Начало: " + start + ", конец: " + end + ", для предмета по ID: " + itemId);
        }

        if (!isAvailableItem) {
            log.info("Недоступен сейчас для бронирования предмет по ID: {}", itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Недоступен сейчас для бронирования предмет по ID: " + itemId);
        }
    }

    public List<BookingResponse> getListAllBookingsForCurrentUser(Long userId, BookingStateFilter bookingStateFilter) {
        boolean isExistBooker = userService.existsUser(userId);

        if (!isExistBooker) {
            log.info("Не найден пользователь пользователь-арендатор по ID: {}, для возврата списка с фильтром {}", userId, bookingStateFilter);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Не найден пользователь пользователь-арендатор по ID: " + userId + ", для возврата списка с фильтром " + bookingStateFilter);
        }

        List<Booking> listBookings;

        switch (bookingStateFilter) {
            case ALL:
                listBookings = bookingRepository.findAllByBookerId(userId);
                break;
            case CURRENT:
                listBookings = bookingRepository.findCurrentByBookerId(userId);
                break;
            case PAST:
                listBookings = bookingRepository.findPastByBookerId(userId);
                break;
            case FUTURE:
                listBookings = bookingRepository.findFutureByBookerId(userId);
                break;
            case WAITING:
                listBookings = bookingRepository.findWaitingByBookerId(userId);
                break;
            case REJECTED:
                listBookings = bookingRepository.findRejectedByBookerId(userId);
                break;
            default:
                log.info("Не существует фильтра {}, пользователь-арендатор по ID: {} запросил бронирования по фильтру", bookingStateFilter, userId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Не существует фильтра " + bookingStateFilter + ", пользователь-арендатор по ID: " + userId + " запросил бронирования по фильтру");
        }

        List<BookingResponse> listBookingResponse = new ArrayList<>();
        for (Booking booking : listBookings) {
            BookingResponse response = toBookingResponse(booking);
            listBookingResponse.add(response);
        }

        return listBookingResponse;
    }

    public List<BookingResponse> getListAllBookingsForCurrentOwner(Long userId, BookingStateFilter bookingStateFilter) {
        boolean isExistOwner = userService.existsUser(userId);

        if (!isExistOwner) {
            log.info("Не найден пользователь пользователь-хозяин по ID: {}, для возврата списка с фильтром {}", userId, bookingStateFilter);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Не найден пользователь пользователь-хозяин по ID: " + userId + ", для возврата списка с фильтром " + bookingStateFilter);
        }

        List<Booking> listBookings;

        switch (bookingStateFilter) {
            case ALL:
                listBookings = bookingRepository.findAllByOwnerId(userId);
                break;
            case CURRENT:
                listBookings = bookingRepository.findCurrentByOwnerId(userId);
                break;
            case PAST:
                listBookings = bookingRepository.findPastByOwnerId(userId);
                break;
            case FUTURE:
                listBookings = bookingRepository.findFutureByOwnerId(userId);
                break;
            case WAITING:
                listBookings = bookingRepository.findWaitingByOwnerId(userId);
                break;
            case REJECTED:
                listBookings = bookingRepository.findRejectedByOwnerId(userId);
                break;
            default:
                log.info("Не существует фильтра {}, хозяина по ID: {} запросил бронирования по фильтру", bookingStateFilter, userId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Не существует фильтра " + bookingStateFilter + ", хозяина по ID: " + userId + " запросил бронирования по фильтру");
        }

        List<BookingResponse> listBookingResponse = new ArrayList<>();
        for (Booking booking : listBookings) {
            BookingResponse response = toBookingResponse(booking);
            listBookingResponse.add(response);
        }

        return listBookingResponse;
    }

    public List<Booking> getBookingPastByBookerIdAndItemId(Long bookerId, Long itemId) {
        List<Booking> listBookings = bookingRepository.findPastByBookerIdAndItemId(bookerId, itemId);

        return listBookings;
    }
}