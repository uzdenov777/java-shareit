package ru.practicum.shareit.booking.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import ru.practicum.shareit.util.MyPageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    private final UserService userService;
    private final ItemService itemService;

    public BookingResponse add(Long bookerId, BookingRequest bookingRequest) throws ResponseStatusException {
        Booking bookingEntity = toBooking(bookerId, bookingRequest);

        checkPossibilityBooking(bookingEntity);

        Booking saveBookingEntity = bookingRepository.save(bookingEntity);
        return toBookingResponse(saveBookingEntity);
    }

    public BookingResponse confirmingOrRejectingBookingRequest(Long userId, Long bookingId, Boolean approved) throws ResponseStatusException {
        Booking booking = getBooking(bookingId);

        Item item = booking.getItem();

        Long ownerId = item.getOwner().getId();
        if (!userId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Пользователь по ID: " + userId + " не может изменить статус бронирования по ID: " + bookingId + ", потому что не является владельцем вещи");
        }

        BookingStatus status = booking.getStatus();
        if (!BookingStatus.WAITING.equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Владелец по ID: " + userId + " не может поменять статус бронирования вещи после принятия решения.");
        }

        if (approved) {
            booking.setStatus(BookingStatus.APPROVED);
        } else {
            booking.setStatus(BookingStatus.REJECTED);
        }

        Booking saveBookingEntity = bookingRepository.save(booking);
        return toBookingResponse(saveBookingEntity);
    }

    public BookingResponse getBookingById(Long userId, Long bookingId) throws ResponseStatusException {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "При запросе на возвращение бронирование не найдено по ID: " + bookingId);
        }

        Booking booking = bookingOpt.get();

        boolean isOwnerOrBooker = isOwnerOrBooker(booking, userId);
        if (!isOwnerOrBooker) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Запрос на возвращение бронирования по ID: " + bookingId + " может только хозяин вещи, либо автор бронирования");
        }

        return toBookingResponse(booking);
    }

    public List<BookingResponse> getListAllBookingsForCurrentUser(Long userId, BookingStateFilter bookingStateFilter, int from, int size) throws ResponseStatusException {
        boolean isExistBooker = userService.existsUser(userId);

        if (!isExistBooker) {
            log.error("Не найден пользователь пользователь-арендатор по ID: {}, для возврата списка с фильтром {}", userId, bookingStateFilter);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Не найден пользователь пользователь-арендатор по ID: " + userId + ", для возврата списка с фильтром " + bookingStateFilter);
        }

        MyPageRequest pageRequest = new MyPageRequest(from, size);
        Page<Booking> page;

        switch (bookingStateFilter) {
            case ALL:
                page = bookingRepository.findAllByBookerIdOrderByIdDesc(pageRequest, userId);
                break;
            case CURRENT:
                page = bookingRepository.findCurrentByBookerId(pageRequest, userId);
                break;
            case PAST:
                page = bookingRepository.findPastByBookerId(pageRequest, userId);
                break;
            case FUTURE:
                page = bookingRepository.findFutureByBookerId(pageRequest, userId);
                break;
            case WAITING:
                page = bookingRepository.findWaitingByBookerId(pageRequest, userId);
                break;
            case REJECTED:
                page = bookingRepository.findRejectedByBookerId(pageRequest, userId);
                break;
            default:
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Не существует фильтра " + bookingStateFilter + ", пользователь-арендатор по ID: " + userId + " запросил бронирования по фильтру");
        }

        List<Booking> listBookings = page.getContent();

        List<BookingResponse> listBookingResponse = new ArrayList<>();

        for (Booking booking : listBookings) {
            BookingResponse response = toBookingResponse(booking);
            listBookingResponse.add(response);
        }

        return listBookingResponse;
    }

    public List<BookingResponse> getListAllBookingsForCurrentOwner(Long userId, BookingStateFilter bookingStateFilter, int from, int size) throws ResponseStatusException {
        boolean isExistOwner = userService.existsUser(userId);

        if (!isExistOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Не найден пользователь пользователь-хозяин по ID: " + userId + ", для возврата списка с фильтром " + bookingStateFilter);
        }

        MyPageRequest pageRequest = new MyPageRequest(from, size);
        Page<Booking> page;

        switch (bookingStateFilter) {
            case ALL:
                page = bookingRepository.findAllByOwnerId(pageRequest, userId);
                break;
            case CURRENT:
                page = bookingRepository.findCurrentByOwnerId(pageRequest, userId);
                break;
            case PAST:
                page = bookingRepository.findPastByOwnerId(pageRequest, userId);
                break;
            case FUTURE:
                page = bookingRepository.findFutureByOwnerId(pageRequest, userId);
                break;
            case WAITING:
                page = bookingRepository.findWaitingByOwnerId(pageRequest, userId);
                break;
            case REJECTED:
                page = bookingRepository.findRejectedByOwnerId(pageRequest, userId);
                break;
            default:
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Не существует фильтра " + bookingStateFilter + ", хозяина по ID: " + userId + " запросил бронирования по фильтру");
        }

        List<Booking> listBookings = page.getContent();

        List<BookingResponse> listBookingResponse = new ArrayList<>();

        for (Booking booking : listBookings) {
            BookingResponse response = toBookingResponse(booking);
            listBookingResponse.add(response);
        }

        return listBookingResponse;
    }

    public Booking toBooking(Long bookerId, BookingRequest bookingRequest) {

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

    private Booking getBooking(Long bookingId) throws ResponseStatusException {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "При запросе на возвращение бронирование не найдено по ID: " + bookingId + " для изменения статуса");
        }

        return bookingOpt.get();
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

    private BookingResponse toBookingResponse(Booking booking) {
        Long id = booking.getId();
        LocalDateTime start = booking.getStart();
        LocalDateTime end = booking.getEnd();
        BookingStatus status = booking.getStatus();

        Item item = booking.getItem();
        BookingResponse.ItemRes itemResponse = toItemResponse(item);

        User user = booking.getBooker();
        User userResponse = toUserResponse(user);

        BookingResponse bookingResponse = new BookingResponse();
        bookingResponse.setId(id);
        bookingResponse.setStart(start);
        bookingResponse.setEnd(end);
        bookingResponse.setStatus(status);
        bookingResponse.setItem(itemResponse);
        bookingResponse.setBooker(userResponse);

        return bookingResponse;
    }

    private BookingResponse.ItemRes toItemResponse(Item item) {
        Long itemId = item.getId();
        String itemName = item.getName();
        String itemDescription = item.getDescription();

        BookingResponse.ItemRes itemResponse = new BookingResponse.ItemRes();
        itemResponse.setId(itemId);
        itemResponse.setName(itemName);
        itemResponse.setDescription(itemDescription);

        return itemResponse;
    }

    private User toUserResponse(User user) {
        Long userId = user.getId();
        String name = user.getName();
        String email = user.getEmail();

        User userResponse = new User();
        userResponse.setId(userId);
        userResponse.setName(name);
        userResponse.setEmail(email);

        return userResponse;
    }

    private void checkPossibilityBooking(Booking booking) throws ResponseStatusException {
        Item bookingItem = booking.getItem();
        Long itemId = bookingItem.getId();
        User ownerItem = bookingItem.getOwner();
        User bookerItem = booking.getBooker();
        Long ownerId = ownerItem.getId();
        Long bookerId = bookerItem.getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = booking.getStart();
        LocalDateTime end = booking.getEnd();

        boolean isEqualsOwnerAndBooker = ownerId.equals(bookerId);
        boolean isEndAfterNow = end.isAfter(now);
        boolean isStartAfterNow = start.isAfter(now);
        boolean isEndEqualStart = start.isEqual(end);
        boolean isEndAfterStart = end.isAfter(start);
        boolean isAvailableItem = bookingItem.getAvailable();

        if (isEqualsOwnerAndBooker) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Владелец по ID: " + ownerId + " не может сам у себя забронировать вещь по ID: " + bookingItem.getId());
        }

        if (!isEndAfterNow) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Конец времени бронирования не может быть в прошлом! Конец: " + end + ", для предмета по ID: " + itemId);
        }

        if (!isStartAfterNow) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Начало времени бронирования не может быть в прошлом! Начало: " + start + ", для предмета по ID: " + itemId);
        }

        if (isEndEqualStart) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Конец времени бронирования не может быть равен началу! Начало: " + start + ", конец: " + end + ", для предмета по ID: " + itemId);
        }

        if (!isEndAfterStart) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Конец времени бронирования не может быть раньше начала! Начало: " + start + ", конец: " + end + ", для предмета по ID: " + itemId);
        }

        if (!isAvailableItem) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Недоступен сейчас для бронирования предмет по ID: " + itemId);
        }
    }
}