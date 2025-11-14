package ru.practicum.shareit.booking.service;

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
        BookingStatus status = booking.getStatus();

        Item item = booking.getItem();

        User owner = item.getOwner();
        Long ownerId = owner.getId();

        if (!userId.equals(ownerId)) {
            log.error("Пользователь по ID: {} не может изменить статус бронирования по ID: {}, потому что не является владельцем вещи", userId, bookingId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь по ID: " + userId + " не может изменить статус бронирования по ID: " + bookingId + ", потому что не является владельцем вещи");
        }

        if (!BookingStatus.WAITING.equals(status)) {
            log.error("Владелец по iD: {} не может поменять статус бронирования вещи после принятия решения.", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Владелец по iD: " + userId + "не может поменять статус бронирования вещи после принятия решения.");
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
            log.error("При запросе на возвращение бронирование не найдено по ID: {}", bookingId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "При запросе на возвращение бронирование не найдено по ID: " + bookingId);
        }

        Booking booking = bookingOpt.get();

        boolean isOwnerOrBooker = isOwnerOrBooker(booking, userId);
        if (!isOwnerOrBooker) {
            log.error("Запрос на возвращение бронирования по ID: {} может только хозяин вещи, либо автор бронирования", bookingId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Запрос на возвращение бронирования по ID: " + bookingId + " может только хозяин вещи, либо автор бронирования");
        }

        BookingResponse bookingResponse = toBookingResponse(booking);
        return bookingResponse;
    }

    private Booking getBooking(Long bookingId) throws ResponseStatusException {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            log.error("При запросе на возвращение бронирование не найдено по ID: {} для изменения статуса", bookingId);
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

        if (isEqualsOwnerAndBooker){
            log.error("Владелец по ID: {} не может сам у себя забронировать вещь по ID: {}", ownerId, bookingItem.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Владелец по ID: " + ownerId + "не может сам у себя забронировать вещь по ID: " + bookingItem.getId());
        }

        if (!isEndAfterNow) {
            log.error("Конец времени бронирования не может быть в прошлом! Конец: {}, для предмета по ID: {} ", end, itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Конец времени бронирования не может быть в прошлом! Конец: " + end + ", для предмета по ID: " + itemId);
        }

        if (!isStartAfterNow) {
            log.error("Начало времени бронирования не может быть в прошлом! Начало: {}, для предмета по ID: {} ", start, itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Начало времени бронирования не может быть в прошлом! Начало: " + start + ", для предмета по ID: " + itemId);
        }

        if (isEndEqualStart) {
            log.error("Конец времени бронирования не может быть равен началу! Начало: {}, конец: {}, для предмета по ID: {} ", start, end, itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Конец времени бронирования не может быть равен началу! Начало: " + start + ", конец: " + end + ", для предмета по ID: " + itemId);
        }

        if (!isEndAfterStart) {
            log.error("Конец времени бронирования не может быть раньше начала! Начало: {}, конец: {}, для предмета по ID: {} ", start, end, itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Конец времени бронирования не может быть раньше начала! Начало: " + start + ", конец: " + end + ", для предмета по ID: " + itemId);
        }

        if (!isAvailableItem) {
            log.error("Недоступен сейчас для бронирования предмет по ID: {}", itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Недоступен сейчас для бронирования предмет по ID: " + itemId);
        }
    }

    public List<BookingResponse> getListAllBookingsForCurrentUser(Long userId, BookingStateFilter bookingStateFilter, int from, int size) {
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
                log.error("Не существует фильтра {}, пользователь-арендатор по ID: {} запросил бронирования по фильтру", bookingStateFilter, userId);
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

    public List<BookingResponse> getListAllBookingsForCurrentOwner(Long userId, BookingStateFilter bookingStateFilter, int from, int size) {
        boolean isExistOwner = userService.existsUser(userId);

        if (!isExistOwner) {
            log.error("Не найден пользователь пользователь-хозяин по ID: {}, для возврата списка с фильтром {}", userId, bookingStateFilter);
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
                log.error("Не существует фильтра {}, хозяина по ID: {} запросил бронирования по фильтру", bookingStateFilter, userId);
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

    public List<Booking> getBookingPastByBookerIdAndItemId(Long bookerId, Long itemId) {
        List<Booking> listBookings = bookingRepository.findPastByBookerIdAndItemId(bookerId, itemId);

        return listBookings;
    }
}