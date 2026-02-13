package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class BookingServiceUnitTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemService itemService;

    @InjectMocks
    private BookingService bookingService;

    private User booker;

    private User owner;

    private Item item;

    private BookingRequest bookingRequest;

    private Booking booking;

    @Captor
    private ArgumentCaptor<Booking> bookingArgumentCaptor;

    @BeforeEach
    void setUp() {
        booker = new User();
        booker.setId(1L);

        owner = new User();
        owner.setId(2L);

        item = new Item();
        item.setId(1L);
        item.setAvailable(true);
        item.setOwner(owner);

        bookingRequest = new BookingRequest();
        bookingRequest.setItemId(item.getId());
        bookingRequest.setStart(LocalDateTime.now().plusMinutes(1));
        bookingRequest.setEnd(LocalDateTime.now().plusDays(1));
    }

    @Test
    void add_whenRequestValid_thenSaveBooker() {
        //given
        Long bookerId = booker.getId();
        Long itemId = bookingRequest.getItemId();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(userService.getUserById(bookerId)).thenReturn(booker);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation ->
                {
                    Booking save = invocation.getArgument(0);
                    save.setId(1L);
                    return save;
                });

        //when
        BookingResponse actualBookingresponse = bookingService.add(bookerId, bookingRequest);

        //then
        //ожидаемые значения
        boolean isAvailableItemExpected = item.getAvailable();
        Long itemIdExpected = item.getId();
        Long ownerIdExpected = owner.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //ловим Booking уже на этапе сохранения в бд
        verify(bookingRepository).save(bookingArgumentCaptor.capture());
        Booking savedBooking = bookingArgumentCaptor.getValue();

        //проверяем Booking
        User savedBooker = savedBooking.getBooker();
        Item savedItem = savedBooking.getItem();
        User savedOwner = item.getOwner();

        Long savedItemId = savedItem.getId();
        boolean savedIsAvailableItem = savedItem.getAvailable();
        Long savedOwnerId = savedOwner.getId();
        Long savedBookerId = savedBooker.getId();
        LocalDateTime savedStartDate = savedBooking.getStart();
        LocalDateTime savedEndDate = savedBooking.getEnd();

        assertEquals(itemIdExpected, savedItemId);
        assertEquals(isAvailableItemExpected, savedIsAvailableItem);
        assertEquals(ownerIdExpected, savedOwnerId);
        assertEquals(bookerIdExpected, savedBookerId);
        assertEquals(startDateExpected, savedStartDate);
        assertEquals(endDateExpected, savedEndDate);

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
    }

    @Test
    void add_whenItemOwnerIsBooker_thenThrowResponseStatusException() {
        //given
        Long bookerId = owner.getId();// делаем бронирующем владельца вещи
        Long itemId = bookingRequest.getItemId();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(userService.getUserById(bookerId)).thenReturn(owner);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingService.add(bookerId, bookingRequest));

        //then
        HttpStatus statusExpected = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Владелец по ID: " + bookerId + " не может сам у себя забронировать вещь по ID: " + itemId + "\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void add_whenEndBookingInPast_thenThrowResponseStatusException() {
        //given
        bookingRequest.setEnd(LocalDateTime.now().minusDays(1)); //сделали конец бронирования в прошлом

        Long bookerId = booker.getId();
        Long itemId = bookingRequest.getItemId();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(userService.getUserById(bookerId)).thenReturn(booker);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingService.add(bookerId, bookingRequest));

        //then
        HttpStatus statusExpected = HttpStatus.BAD_REQUEST;
        LocalDateTime end = bookingRequest.getEnd();
        String exceptionMessage = "400 BAD_REQUEST \"Конец времени бронирования не может быть в прошлом! Конец: " + end + ", для предмета по ID: " + itemId + "\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void add_whenStartingBookingInPast_thenThrowResponseStatusException() {
        //given
        bookingRequest.setStart(LocalDateTime.now().minusDays(1)); //сделали начало бронирования в прошлом

        Long bookerId = booker.getId();
        Long itemId = bookingRequest.getItemId();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(userService.getUserById(bookerId)).thenReturn(booker);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingService.add(bookerId, bookingRequest));

        //then
        HttpStatus statusExpected = HttpStatus.BAD_REQUEST;
        LocalDateTime start = bookingRequest.getStart();
        String exceptionMessage = "400 BAD_REQUEST \"Начало времени бронирования не может быть в прошлом! Начало: " + start + ", для предмета по ID: " + itemId + "\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void add_whenEndEqualStart_thenThrowResponseStatusException() {
        //given
        LocalDateTime startRequest = bookingRequest.getStart();
        bookingRequest.setEnd(startRequest); //сделали конец равный началу бронирования

        Long bookerId = booker.getId();
        Long itemId = bookingRequest.getItemId();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(userService.getUserById(bookerId)).thenReturn(booker);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingService.add(bookerId, bookingRequest));

        //then
        HttpStatus statusExpected = HttpStatus.BAD_REQUEST;
        LocalDateTime end = bookingRequest.getEnd();
        String exceptionMessage = "400 BAD_REQUEST \"Конец времени бронирования не может быть равен началу! Начало: " + startRequest
                + ", конец: " + end + ", для предмета по ID: " + itemId + "\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void add_whenEndAfterStart_thenThrowResponseStatusException() {
        //given
        //специально ушли в будущее чтобы не словить другие негативные ситуации
        bookingRequest.setStart(LocalDateTime.now().plusDays(10));

        LocalDateTime startRequest = bookingRequest.getStart();
        bookingRequest.setEnd(startRequest.minusDays(1)); //сделали конец раньше начала бронирования

        Long bookerId = booker.getId();
        Long itemId = bookingRequest.getItemId();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(userService.getUserById(bookerId)).thenReturn(booker);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingService.add(bookerId, bookingRequest));

        //then
        HttpStatus statusExpected = HttpStatus.BAD_REQUEST;
        LocalDateTime end = bookingRequest.getEnd();
        String exceptionMessage = "400 BAD_REQUEST \"Конец времени бронирования не может быть раньше начала! Начало: " + startRequest
                + ", конец: " + end + ", для предмета по ID: " + itemId + "\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void add_whenNotAvailableItem_themThrowResponseStatusException() {
        //given
        item.setAvailable(false); //теперь вещь не доступна

        Long bookerId = booker.getId();
        Long itemId = bookingRequest.getItemId();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(userService.getUserById(bookerId)).thenReturn(booker);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingService.add(bookerId, bookingRequest));

        //then
        HttpStatus statusExpected = HttpStatus.BAD_REQUEST;
        String exceptionMessage = "400 BAD_REQUEST \"Недоступен сейчас для бронирования предмет по ID: " + itemId + "\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void confirmingOrRejectingBookingRequest_whenRequestValidAndApprovedTrue_thenSaveBookingStatusApproved() {
        //giver
        setUpBooking(bookingRequest);// это нужно чтобы удобный нам BookingRequest стал Booking и находить могли типа в бд его

        Boolean approved = true; //решение одобрить бронирование
        Long ownerId = owner.getId();
        Long bookingId = booking.getId();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        //when
        BookingResponse actualBookingresponse = bookingService.confirmingOrRejectingBookingRequest(ownerId, bookingId, approved);

        //then
        BookingStatus statusExpected = BookingStatus.APPROVED;

        assertEquals(statusExpected, actualBookingresponse.getStatus());
    }

    @Test
    void confirmingOrRejectingBookingRequest_whenRequestValidAndApprovedFalse_thenSaveBookingStatusRejected() {
        //giver
        setUpBooking(bookingRequest);// это нужно чтобы удобный нам BookingRequest стал Booking и находить могли типа в бд его

        Boolean approved = false; // решение не одобрить бронирование
        Long ownerId = owner.getId();
        Long bookingId = booking.getId();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        //when
        BookingResponse actualBookingresponse = bookingService.confirmingOrRejectingBookingRequest(ownerId, bookingId, approved);

        //then
        BookingStatus statusExpected = BookingStatus.REJECTED;

        assertEquals(statusExpected, actualBookingresponse.getStatus());
    }

    @Test
    void confirmingOrRejectingBookingRequest_whenBookingByIdNotFound_thenThrowResponseStatusException() {
        //giver
        setUpBooking(bookingRequest);// это нужно чтобы удобный нам BookingRequest стал Booking и находить могли типа в бд его

        Boolean approved = false; // решение не одобрить бронирование
        Long ownerId = owner.getId();
        Long bookingId = booking.getId();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class,
                () -> bookingService.confirmingOrRejectingBookingRequest(ownerId, bookingId, approved));

        //then
        HttpStatus statusExpected = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"При запросе на возвращение бронирование не найдено по ID: " + bookingId + " для изменения статуса\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository).findById(bookingId);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void confirmingOrRejectingBookingRequest_whenNonOwnerChangesBookingStatus_thenThrowResponseStatusException() {
        //giver
        setUpBooking(bookingRequest);// это нужно чтобы удобный нам BookingRequest стал Booking и находить могли типа в бд его

        Boolean approved = true;
        Long bookerId = booker.getId(); // не хозяин вещи будет пробовать изменить статус бронирования
        Long bookingId = booking.getId();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class,
                () -> bookingService.confirmingOrRejectingBookingRequest(bookerId, bookingId, approved));

        //then
        HttpStatus statusExpected = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Пользователь по ID: " + bookerId + " не может изменить статус бронирования по ID: " + bookingId + ", потому что не является владельцем вещи\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository).findById(bookingId);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void confirmingOrRejectingBookingRequest_whenOwnerChangesNonWaitingBookingStatus_thenThrowResponseStatusException() {
        //giver
        setUpBooking(bookingRequest);// это нужно чтобы удобный нам BookingRequest стал Booking и находить могли типа в бд его
        booking.setStatus(BookingStatus.APPROVED);// изменили статус специально

        Boolean approved = true;
        Long ownerId = owner.getId();
        Long bookingId = booking.getId();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class,
                () -> bookingService.confirmingOrRejectingBookingRequest(ownerId, bookingId, approved));

        //then
        HttpStatus statusExpected = HttpStatus.BAD_REQUEST;
        String exceptionMessage = "400 BAD_REQUEST \"Владелец по ID: " + ownerId + " не может поменять статус бронирования вещи после принятия решения.\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository).findById(bookingId);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void getBookingById_whenOwnerRequests_thenReturnBookingResponse() {
        //giver
        setUpBooking(bookingRequest);// это нужно чтобы удобный нам BookingRequest стал Booking и находить могли типа в бд его

        Long ownerId = owner.getId();// хозяин вещи запрашивает бронирование
        Long bookingId = booking.getId();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        //when
        BookingResponse actualBookingresponse = bookingService.getBookingById(ownerId, bookingId);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
    }

    @Test
    void getBookingById_whenBookerRequests_thenReturnBookingResponse() {
        //giver
        setUpBooking(bookingRequest);// это нужно чтобы удобный нам BookingRequest стал Booking и находить могли типа в бд его

        Long bookerId = booker.getId();// арендатор вещи запрашивает бронирование
        Long bookingId = booking.getId();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        //when
        BookingResponse actualBookingresponse = bookingService.getBookingById(bookerId, bookingId);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
    }

    @Test
    void getBookingById_whenBookingNotFound_thenThrowResponseStatusException() {
        //giver
        Long bookerId = booker.getId();
        Long nonExistentBookingId = 777L;// не существующий ID

        when(bookingRepository.findById(nonExistentBookingId)).thenReturn(Optional.empty());

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class,
                () -> bookingService.getBookingById(bookerId, nonExistentBookingId));

        //then
        HttpStatus statusExpected = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"При запросе на возвращение бронирование не найдено по ID: " + nonExistentBookingId + "\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository).findById(nonExistentBookingId);
    }

    @Test
    void getBookingById_whenNeitherOwnerNorBookerRequests_thenThrowResponseStatusException() {
        //giver
        setUpBooking(bookingRequest);

        Long userId = 777L; // ID пользователя который не владелец и не арендатор вещи
        Long bookingId = booking.getId();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class,
                () -> bookingService.getBookingById(userId, bookingId));

        //then
        HttpStatus statusExpected = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Запрос на возвращение бронирования по ID: " + bookingId + " может только хозяин вещи, либо автор бронирования\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(bookingRepository).findById(bookingId);
    }

    @Test
    void getListAllBookingsForCurrentUser_whenBookingStateFilterAll_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long bookerId = booker.getId();
        BookingStateFilter filter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(bookerId)).thenReturn(true);
        when(bookingRepository.findAllByBookerIdOrderByIdDesc(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentUser(bookerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findAllByBookerIdOrderByIdDesc(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentUser_whenBookingStateFilterCurrent_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long bookerId = booker.getId();
        BookingStateFilter filter = BookingStateFilter.CURRENT;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(bookerId)).thenReturn(true);
        when(bookingRepository.findCurrentByBookerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentUser(bookerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findCurrentByBookerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentUser_whenBookingStateFilterPast_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long bookerId = booker.getId();
        BookingStateFilter filter = BookingStateFilter.PAST;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(bookerId)).thenReturn(true);
        when(bookingRepository.findPastByBookerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentUser(bookerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findPastByBookerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentUser_whenBookingStateFilterFuture_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long bookerId = booker.getId();
        BookingStateFilter filter = BookingStateFilter.FUTURE;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(bookerId)).thenReturn(true);
        when(bookingRepository.findFutureByBookerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentUser(bookerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findFutureByBookerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentUser_whenBookingStateFilterWaiting_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long bookerId = booker.getId();
        BookingStateFilter filter = BookingStateFilter.WAITING;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(bookerId)).thenReturn(true);
        when(bookingRepository.findWaitingByBookerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentUser(bookerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findWaitingByBookerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentUser_whenBookingStateFilterRejected_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long bookerId = booker.getId();
        BookingStateFilter filter = BookingStateFilter.REJECTED;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(bookerId)).thenReturn(true);
        when(bookingRepository.findRejectedByBookerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentUser(bookerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findRejectedByBookerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentUser_whenUserNotFount_thenThrowResponseStatusException() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long bookerId = 7777L; //не существующий пользователь, который произвел запрос
        BookingStateFilter filter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        when(userService.existsUser(bookerId)).thenReturn(false); //не нашли пользователя в бд

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingService.getListAllBookingsForCurrentUser(bookerId, filter, from, size));

        //then
        HttpStatus statusExpected = HttpStatus.FORBIDDEN;
        String exceptionMessageExpected = "403 FORBIDDEN \"Не найден пользователь пользователь-арендатор по ID: " + bookerId + ", для возврата списка с фильтром " + filter + "\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessageExpected, resException.getMessage());
        verify(userService, times(1)).existsUser(bookerId);
        verify(bookingRepository, never()).findAllByBookerIdOrderByIdDesc(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentOwner_whenBookingStateFilterAll_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long ownerId = owner.getId();
        BookingStateFilter filter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(ownerId)).thenReturn(true);
        when(bookingRepository.findAllByOwnerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentOwner(ownerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findAllByOwnerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentOwner_whenBookingStateFilterCurrent_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long ownerId = owner.getId();
        BookingStateFilter filter = BookingStateFilter.CURRENT;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(ownerId)).thenReturn(true);
        when(bookingRepository.findCurrentByOwnerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentOwner(ownerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findCurrentByOwnerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentOwner_whenBookingStateFilterPast_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long ownerId = owner.getId();
        BookingStateFilter filter = BookingStateFilter.PAST;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(ownerId)).thenReturn(true);
        when(bookingRepository.findPastByOwnerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentOwner(ownerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findPastByOwnerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentOwner_whenBookingStateFilterFuture_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long ownerId = owner.getId();
        BookingStateFilter filter = BookingStateFilter.FUTURE;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(ownerId)).thenReturn(true);
        when(bookingRepository.findFutureByOwnerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentOwner(ownerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findFutureByOwnerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentOwner_whenBookingStateFilterWaiting_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long ownerId = owner.getId();
        BookingStateFilter filter = BookingStateFilter.WAITING;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(ownerId)).thenReturn(true);
        when(bookingRepository.findWaitingByOwnerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentOwner(ownerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findWaitingByOwnerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentOwner_whenBookingStateFilterRejected_thenNotEmptyList() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long ownerId = owner.getId();
        BookingStateFilter filter = BookingStateFilter.REJECTED;
        int from = 0;
        int size = 10;

        List<Booking> bookingsExpected = List.of(booking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsExpected);

        when(userService.existsUser(ownerId)).thenReturn(true);
        when(bookingRepository.findRejectedByOwnerId(any(), anyLong())).thenReturn(bookingPage);

        //when
        List<BookingResponse> resBookings = bookingService.getListAllBookingsForCurrentOwner(ownerId, filter, from, size);
        BookingResponse actualBookingresponse = resBookings.get(0);

        //then
        //ожидаемые значения
        Long itemIdExpected = item.getId();
        Long bookerIdExpected = booker.getId();
        BookingStatus statusExpected = BookingStatus.WAITING;
        LocalDateTime startDateExpected = bookingRequest.getStart();
        LocalDateTime endDateExpected = bookingRequest.getEnd();

        //проверяем BookingResponse который нам вернули как ответ на запрос на сохранение
        User actualBooker = actualBookingresponse.getBooker();
        BookingResponse.ItemRes actualItem = actualBookingresponse.getItem();

        Long actualItemId = actualItem.getId();
        Long actualBookerId = actualBooker.getId();
        BookingStatus actualBookingStatus = actualBookingresponse.getStatus();
        LocalDateTime actualStartDate = actualBookingresponse.getStart();
        LocalDateTime actualEndDate = actualBookingresponse.getEnd();

        assertEquals(1, resBookings.size());
        assertEquals(itemIdExpected, actualItemId);
        assertEquals(bookerIdExpected, actualBookerId);
        assertEquals(statusExpected, actualBookingStatus);
        assertEquals(startDateExpected, actualStartDate);
        assertEquals(endDateExpected, actualEndDate);
        verify(bookingRepository).findRejectedByOwnerId(any(), anyLong());
    }

    @Test
    void getListAllBookingsForCurrentOwner_whenUserNotFount_thenThrowResponseStatusException() {
        //given
        setUpBooking(bookingRequest);

        //параметры для запроса
        Long ownerId = 7777L; //не существующий пользователь, который произвел запрос
        BookingStateFilter filter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        when(userService.existsUser(ownerId)).thenReturn(false); //не нашли пользователя в бд

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingService.getListAllBookingsForCurrentOwner(ownerId, filter, from, size));

        //then
        HttpStatus statusExpected = HttpStatus.FORBIDDEN;
        String exceptionMessageExpected = "403 FORBIDDEN \"Не найден пользователь пользователь-хозяин по ID: " + ownerId + ", для возврата списка с фильтром " + filter + "\"";

        assertEquals(statusExpected, resException.getStatusCode());
        assertEquals(exceptionMessageExpected, resException.getMessage());
        verify(userService, times(1)).existsUser(ownerId);
        verify(bookingRepository, never()).findAllByBookerIdOrderByIdDesc(any(), anyLong());
    }

    private void setUpBooking(BookingRequest bookingRequest) {

        Long bookerId = booker.getId();
        Long itemId = bookingRequest.getItemId();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(userService.getUserById(bookerId)).thenReturn(booker);

        booking = bookingService.toBooking(booker.getId(), bookingRequest);
        booking.setId(1L);
    }
}