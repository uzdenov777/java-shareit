package ru.practicum.shareit.booking.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.booking.model.dto.BookingRequest;
import ru.practicum.shareit.booking.model.dto.BookingResponse;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.booking.service.BookingStateFilter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerUnitTest {

    private BookingRequest bookingRequest;
    private BookingResponse bookingResponse;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        bookingRequest = new BookingRequest();
        bookingResponse = new BookingResponse();
    }

    @Test
    void addBooking_whenPossibilityBooking_thenBookingSaved() {
        //given
        Long bookerId = 1L;

        when(bookingService.add(bookerId, bookingRequest)).thenReturn(bookingResponse);

        //when
        BookingResponse bookingRes = bookingController.addBooking(bookerId, bookingRequest);

        //then
        assertEquals(bookingResponse, bookingRes);
        verify(bookingService).add(bookerId, bookingRequest);
    }

    @Test
    void addBooking_whenNotPossibilityBooking_thenThrowsException() {
        //given
        Long bookerId = 1L;

        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "Booking not found";
        ResponseStatusException exception = new ResponseStatusException(status, exceptionMessage);

        when(bookingService.add(bookerId, bookingRequest)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingController.addBooking(bookerId, bookingRequest));

        //then
        assertEquals(exception, resException);
        verify(bookingService).add(bookerId, bookingRequest);
    }

    @Test
    void confirmingOrRejectingBookingRequest_whenAllValid_thenBookingSavedStatus() {
        //given
        Long userId = 1L;
        Long bookingId = 77L;
        boolean approved = true;

        when(bookingService.confirmingOrRejectingBookingRequest(userId, bookingId, approved)).thenReturn(bookingResponse);

        //when
        BookingResponse bookingResp = bookingController.confirmingOrRejectingBookingRequest(userId, bookingId, approved);

        //then
        assertEquals(bookingResponse, bookingResp);
        verify(bookingService).confirmingOrRejectingBookingRequest(userId, bookingId, approved);
    }

    @Test
    void confirmingOrRejectingBookingRequest_whenNotValid_thenThrowsException() {
        //given
        Long userId = 1L;
        Long bookingId = 77L;
        boolean approved = true;

        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "Какая-то ошибка";
        ResponseStatusException exception = new ResponseStatusException(status, exceptionMessage);

        when(bookingService.confirmingOrRejectingBookingRequest(userId, bookingId, approved)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class
                , () -> bookingController.confirmingOrRejectingBookingRequest(userId, bookingId, approved));

        assertEquals(exception, resException);
        verify(bookingService).confirmingOrRejectingBookingRequest(userId, bookingId, approved);
    }

    @Test
    void getBookingById_whenBookingExists_thenReturnBookingResponse() {
        //given
        Long userId = 1L;
        Long bookingId = 77L;

        when(bookingService.getBookingById(userId, bookingId)).thenReturn(bookingResponse);

        //when
        BookingResponse bookingRes = bookingController.getBookingById(userId, bookingId);

        //then
        assertEquals(bookingResponse, bookingRes);
        verify(bookingService).getBookingById(userId, bookingId);
    }

    @Test
    void getBookingById_whenBookingNotExists_thenThrowsException() {
        //given
        Long userId = 1L;
        Long bookingId = 77L;

        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "Какая-то ошибка";
        ResponseStatusException exception = new ResponseStatusException(status, exceptionMessage);

        when(bookingService.getBookingById(userId, bookingId)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> bookingController.getBookingById(userId, bookingId));

        //then
        assertEquals(exception, resException);
        verify(bookingService).getBookingById(userId, bookingId);
    }

    @Test
    void getListAllBookingsForCurrentBooker_whenRequestValid_thenReturnResponse200AndBodyNotEmptyList() {
        //given
        Long userId = 1L;
        String stateToString = "all";
        BookingStateFilter state = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;
        List<BookingResponse> bookings = List.of(new BookingResponse(), new BookingResponse());

        when(bookingService.getListAllBookingsForCurrentUser(userId, state, from, size)).thenReturn(bookings);

        //when
        ResponseEntity<Object> res = bookingController.getListAllBookingsForCurrentBooker(userId, stateToString, from, size);

        //then
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(bookings, res.getBody());
        verify(bookingService).getListAllBookingsForCurrentUser(userId, state, from, size);
    }

    @Test
    void getListAllBookingsForCurrentBooker_whenRequestNotValid_thenThrowsResponseStatusException() {
        //given
        Long userId = 1L;
        String stateToString = "all";
        BookingStateFilter state = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "Какая-то ошибка";
        ResponseStatusException exception = new ResponseStatusException(status, exceptionMessage);

        when(bookingService.getListAllBookingsForCurrentUser(userId, state, from, size)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class
                , () -> bookingController.getListAllBookingsForCurrentBooker(userId, stateToString, from, size));

        //then
        assertEquals(exception, resException);
        verify(bookingService).getListAllBookingsForCurrentUser(userId, state, from, size);
    }

    @Test
    void getListAllBookingsForCurrentBooker_whenStateNotExist_thenThrowsResponseStatusException() {
        //given
        Long userId = 1L;
        String stateToString = "NotExistsState";
        int from = 0;
        int size = 10;

        //when
        ResponseEntity<Object> response = bookingController.getListAllBookingsForCurrentBooker(userId, stateToString, from, size);

        //then
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        assertEquals(status, response.getStatusCode());
        verify(bookingService, never()).getListAllBookingsForCurrentUser(any(), any(), anyInt(), anyInt());
    }

    @Test
    void getListBookingsForCurrentOwner_whenRequestValid_thenReturnResponse200AndBodyNotEmptyList() {
        //given
        Long ownerId = 1L;
        String stateToString = "all";
        BookingStateFilter state = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;
        List<BookingResponse> bookings = List.of(new BookingResponse(), new BookingResponse());

        when(bookingService.getListAllBookingsForCurrentOwner(ownerId, state, from, size)).thenReturn(bookings);

        //when
        ResponseEntity<Object> res = bookingController.getListBookingsForCurrentOwner(ownerId, stateToString, from, size);

        //then
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(bookings, res.getBody());
        verify(bookingService).getListAllBookingsForCurrentOwner(ownerId, state, from, size);
    }

    @Test
    void getListBookingsForCurrentOwner_whenRequestNotValid_thenThrowsResponseStatusException() {
        //given
        Long ownerId = 1L;
        String stateToString = "all";
        BookingStateFilter state = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "Какая-то ошибка";
        ResponseStatusException exception = new ResponseStatusException(status, exceptionMessage);

        when(bookingService.getListAllBookingsForCurrentOwner(ownerId, state, from, size)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class
                , () -> bookingController.getListBookingsForCurrentOwner(ownerId, stateToString, from, size));

        //then
        assertEquals(exception, resException);
        verify(bookingService).getListAllBookingsForCurrentOwner(ownerId, state, from, size);
    }

    @Test
    void getListBookingsForCurrentOwner_whenStateNotExist_thenThrowsResponseStatusException() {
        //given
        Long ownerId = 1L;
        String stateToString = "NotExistsState";
        int from = 0;
        int size = 10;

        //when
        ResponseEntity<Object> response = bookingController.getListBookingsForCurrentOwner(ownerId, stateToString, from, size);

        //then
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        assertEquals(status, response.getStatusCode());
        verify(bookingService, never()).getListAllBookingsForCurrentOwner(any(), any(), anyInt(), anyInt());
    }
}