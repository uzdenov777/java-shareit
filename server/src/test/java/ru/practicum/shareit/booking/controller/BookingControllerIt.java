package ru.practicum.shareit.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.model.dto.BookingRequest;
import ru.practicum.shareit.booking.model.dto.BookingResponse;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.booking.service.BookingStateFilter;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(BookingController.class)
class BookingControllerIT {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    private BookingRequest bookingRequest;

    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now().plusDays(1L);
        BookingStatus status = BookingStatus.WAITING;
        User booker = new User(1L, "nameUser", "email@gmail.com");
        BookingResponse.ItemRes itemRes = new BookingResponse.ItemRes(1L, "nameItemRes", "descriptionItemRes");

        bookingRequest = new BookingRequest();
        bookingRequest.setItemId(itemRes.getId());
        bookingRequest.setStart(startTime);
        bookingRequest.setEnd(endTime);

        bookingResponse = new BookingResponse();
        bookingResponse.setStart(startTime);
        bookingResponse.setEnd(endTime);
        bookingResponse.setStatus(status);
        bookingResponse.setBooker(booker);
        bookingResponse.setItem(itemRes);
    }

    @SneakyThrows
    @Test
    void addBooking_whenValidRequest_thenReturnBooking() {
        //given
        Long bookerId = 1L;

        when(bookingService.add(bookerId, bookingRequest))
                .thenAnswer(i ->
                {
                    Long bookingId = 1L;
                    bookingResponse.setId(bookingId);

                    return bookingResponse;
                });

        //when+then
        String responseJson = mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.start").exists())
                .andExpect(jsonPath("$.end").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.booker.id").exists())
                .andExpect(jsonPath("$.booker.name").exists())
                .andExpect(jsonPath("$.booker.email").exists())
                .andExpect(jsonPath("$.item.id").exists())
                .andExpect(jsonPath("$.item.name").exists())
                .andExpect(jsonPath("$.item.description").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //полученный JSON переводим в объект BookingResponse
        BookingResponse jsonToBookingResponse = objectMapper.readValue(responseJson, BookingResponse.class);
        User bookerFromResponse = jsonToBookingResponse.getBooker();
        BookingResponse.ItemRes itemFromResponse = jsonToBookingResponse.getItem();

        //ожидаемые значения
        Long bookingIdExpected = bookingResponse.getId();
        LocalDateTime bookingStartExpected = bookingResponse.getStart();
        LocalDateTime bookingEndExpected = bookingResponse.getEnd();
        BookingStatus bookingStatusExpected = bookingResponse.getStatus();
        Long bookerIdExpected = bookingResponse.getBooker().getId();
        String bookerNameExpected = bookingResponse.getBooker().getName();
        String bookerEmailExpected = bookingResponse.getBooker().getEmail();
        Long itemIdExpected = bookingResponse.getItem().getId();
        String itemNameExpected = bookingResponse.getItem().getName();
        String itemDescriptionExpected = bookingResponse.getItem().getDescription();

        //проверка
        assertEquals(bookingIdExpected, jsonToBookingResponse.getId());
        assertEquals(bookingStartExpected, jsonToBookingResponse.getStart());
        assertEquals(bookingEndExpected, jsonToBookingResponse.getEnd());
        assertEquals(bookingStatusExpected, bookingResponse.getStatus());
        assertEquals(bookerIdExpected, bookerFromResponse.getId());
        assertEquals(bookerNameExpected, bookerFromResponse.getName());
        assertEquals(bookerEmailExpected, bookerFromResponse.getEmail());
        assertEquals(itemIdExpected, itemFromResponse.getId());
        assertEquals(itemNameExpected, itemFromResponse.getName());
        assertEquals(itemDescriptionExpected, itemFromResponse.getDescription());

        verify(bookingService).add(bookerId, bookingRequest);
    }

    @SneakyThrows
    @Test
    void addBooking_whenUserIdIsNull_thenReturnBadRequest() {

        //не передаем в запросе заголовок "X-Sharer-User-Id" == userId = null
        //when+then
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isBadRequest());  // ожидаем 400

        verify(bookingService, never()).add(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void addBooking_whenUserIdIsNotNumber_thenReturnBadRequest() {
        //given
        String notNumber = "notNumber";

        // передаем в запросе "X-Sharer-User-Id" не цифру
        //when+then
        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", notNumber) //передали в заголовок не цифру
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).add(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void addBooking_whenEmptyBody_thenReturnBadRequest() {
        //given
        Long bookerId = 1L;

        //не передаем тело в запросе = body == null
        //when+then
        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isBadRequest());  // ожидаем 400

        verify(bookingService, never()).add(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void addBooking_whenBookingRequestFieldsEmpty_thenReturnBadRequest() {
        //given
        Long bookerId = 1L;

        //не заполняем поля
        BookingRequest bookingRequest = new BookingRequest();

        //when+then
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest))
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isBadRequest());  // ожидаем 400

        verify(bookingService, never()).add(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void confirmingOrRejectingBookingRequest_whenRequestValid_thenChangeStatusAndReturnResponse() {
        //given
        //параметры для запроса
        Long bookerId = 1L;
        Long bookingId = 1L;
        Boolean approved = true;

        when(bookingService.confirmingOrRejectingBookingRequest(bookerId, bookingId, approved))
                .thenAnswer(i -> {
                    bookingResponse.setId(1L);
                    bookingResponse.setStatus(BookingStatus.APPROVED);
                    return bookingResponse;
                });

        //when+then
        String resJson = mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                        .header("X-Sharer-User-Id", bookerId)
                        .param("approved", approved.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.start").exists())
                .andExpect(jsonPath("$.end").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.booker.id").exists())
                .andExpect(jsonPath("$.booker.name").exists())
                .andExpect(jsonPath("$.booker.email").exists())
                .andExpect(jsonPath("$.item.id").exists())
                .andExpect(jsonPath("$.item.name").exists())
                .andExpect(jsonPath("$.item.description").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //полученный JSON переводим в объект BookingResponse
        BookingResponse jsonToBookingResponse = objectMapper.readValue(resJson, BookingResponse.class);
        User bookerFromResponse = jsonToBookingResponse.getBooker();
        BookingResponse.ItemRes itemFromResponse = jsonToBookingResponse.getItem();

        //ожидаемые значения
        Long bookingIdExpected = bookingResponse.getId();
        LocalDateTime bookingStartExpected = bookingResponse.getStart();
        LocalDateTime bookingEndExpected = bookingResponse.getEnd();
        BookingStatus bookingStatusExpected = bookingResponse.getStatus();
        Long bookerIdExpected = bookingResponse.getBooker().getId();
        String bookerNameExpected = bookingResponse.getBooker().getName();
        String bookerEmailExpected = bookingResponse.getBooker().getEmail();
        Long itemIdExpected = bookingResponse.getItem().getId();
        String itemNameExpected = bookingResponse.getItem().getName();
        String itemDescriptionExpected = bookingResponse.getItem().getDescription();

        //проверка
        assertEquals(bookingIdExpected, jsonToBookingResponse.getId());
        assertEquals(bookingStartExpected, jsonToBookingResponse.getStart());
        assertEquals(bookingEndExpected, jsonToBookingResponse.getEnd());
        assertEquals(bookingStatusExpected, bookingResponse.getStatus());
        assertEquals(bookerIdExpected, bookerFromResponse.getId());
        assertEquals(bookerNameExpected, bookerFromResponse.getName());
        assertEquals(bookerEmailExpected, bookerFromResponse.getEmail());
        assertEquals(itemIdExpected, itemFromResponse.getId());
        assertEquals(itemNameExpected, itemFromResponse.getName());
        assertEquals(itemDescriptionExpected, itemFromResponse.getDescription());

        verify(bookingService).confirmingOrRejectingBookingRequest(bookerId, bookingId, approved);
    }

    @SneakyThrows
    @Test
    void confirmingOrRejectingBookingRequest_whenRequestValid_thenReturnBadRequest() {
        //given
        //параметры для запроса
        Long bookingId = 1L;
        boolean approved = true;

        //when+then
        //не передаем в запросе заголовок "X-Sharer-User-Id" = userId == null
        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                        .param("approved", Boolean.toString(approved))).
                andExpect(status().isBadRequest());

        verify(bookingService, never()).confirmingOrRejectingBookingRequest(anyLong(), anyLong(), anyBoolean());
    }

    @SneakyThrows
    @Test
    void confirmingOrRejectingBookingRequest_whenUserIdIsNotNumber_thenReturnBadRequest() {
        //given
        //параметры для запроса
        String bookerId = "ll";
        Long bookingId = 1L;
        boolean approved = true;

        //when+then
        //не передаем в запросе заголовок "X-Sharer-User-Id" = userId == null
        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                        .header("X-Sharer-User-Id", bookerId)
                        .param("approved", Boolean.toString(approved))).
                andExpect(status().isBadRequest());

        verify(bookingService, never()).confirmingOrRejectingBookingRequest(anyLong(), anyLong(), anyBoolean());
    }

    @SneakyThrows
    @Test
    void confirmingOrRejectingBookingRequest_whenBookingIdIsNotNumber_thenReturnBadRequest() {
        //given
        //в путь "/{bookingId}" передаем не цифру, а букву
        String bookingId = "A";
        Long bookerId = 1L;
        boolean approved = true;

        //when+then
        //не передаем в запросе заголовок "X-Sharer-User-Id" = userId == null
        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                        .header("X-Sharer-User-Id", bookerId)
                        .param("approved", Boolean.toString(approved))).
                andExpect(status().isBadRequest());

        verify(bookingService, never()).confirmingOrRejectingBookingRequest(anyLong(), anyLong(), anyBoolean());
    }

    @SneakyThrows
    @Test
    void confirmingOrRejectingBookingRequest_whenApprovedIsNull_thenReturnBadRequest() {
        //given
        Long bookingId = 1L;
        Long bookerId = 1L;

        //when+then
        //не передаем в запросе параметр approved == Boolean approved = null
        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).confirmingOrRejectingBookingRequest(anyLong(), anyLong(), anyBoolean());
    }

    @SneakyThrows
    @Test
    void confirmingOrRejectingBookingRequest_whenApprovedIsNotBoolean_thenReturnBadRequest() {
        //given
        Long bookingId = 1L;
        Long bookerId = 1L;
        String approved = "A";

        //when+then
        //не передаем в запросе параметр approved == Boolean approved = null
        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                        .header("X-Sharer-User-Id", bookerId)
                        .param("approved", approved))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).confirmingOrRejectingBookingRequest(anyLong(), anyLong(), anyBoolean());
    }

    @SneakyThrows
    @Test
    void getBookingById_whenRequestValid_thenReturnBookingResponse() {
        //given
        Long bookingId = 1L;
        Long bookerId = 1L;

        bookingResponse.setId(bookingId);
        when(bookingService.getBookingById(bookerId, bookingId)).thenReturn(bookingResponse);

        //when+then
        String resJson = mockMvc.perform(get("/bookings/{bookingId}", bookingId)
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.start").exists())
                .andExpect(jsonPath("$.end").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.booker.id").exists())
                .andExpect(jsonPath("$.booker.name").exists())
                .andExpect(jsonPath("$.booker.email").exists())
                .andExpect(jsonPath("$.item.id").exists())
                .andExpect(jsonPath("$.item.name").exists())
                .andExpect(jsonPath("$.item.description").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //полученный JSON переводим в объект BookingResponse
        BookingResponse jsonToBookingResponse = objectMapper.readValue(resJson, BookingResponse.class);
        User bookerFromResponse = jsonToBookingResponse.getBooker();
        BookingResponse.ItemRes itemFromResponse = jsonToBookingResponse.getItem();

        //ожидаемые значения
        Long bookingIdExpected = bookingResponse.getId();
        LocalDateTime bookingStartExpected = bookingResponse.getStart();
        LocalDateTime bookingEndExpected = bookingResponse.getEnd();
        BookingStatus bookingStatusExpected = bookingResponse.getStatus();
        Long bookerIdExpected = bookingResponse.getBooker().getId();
        String bookerNameExpected = bookingResponse.getBooker().getName();
        String bookerEmailExpected = bookingResponse.getBooker().getEmail();
        Long itemIdExpected = bookingResponse.getItem().getId();
        String itemNameExpected = bookingResponse.getItem().getName();
        String itemDescriptionExpected = bookingResponse.getItem().getDescription();

        //проверка
        assertEquals(bookingIdExpected, jsonToBookingResponse.getId());
        assertEquals(bookingStartExpected, jsonToBookingResponse.getStart());
        assertEquals(bookingEndExpected, jsonToBookingResponse.getEnd());
        assertEquals(bookingStatusExpected, bookingResponse.getStatus());
        assertEquals(bookerIdExpected, bookerFromResponse.getId());
        assertEquals(bookerNameExpected, bookerFromResponse.getName());
        assertEquals(bookerEmailExpected, bookerFromResponse.getEmail());
        assertEquals(itemIdExpected, itemFromResponse.getId());
        assertEquals(itemNameExpected, itemFromResponse.getName());
        assertEquals(itemDescriptionExpected, itemFromResponse.getDescription());

        verify(bookingService).getBookingById(bookerId, bookingId);
    }

    @SneakyThrows
    @Test
    void getBookingById_whenUserIdIsNull_thenReturnBadRequest() {
        //given
        Long bookingId = 1L;

        //when+then
        //в запросе не передаем заголовок "X-Sharer-User-Id" == userId = Null
        mockMvc.perform(get("/bookings/{bookingId}", bookingId))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getBookingById(anyLong(), anyLong());
    }

    @SneakyThrows
    @Test
    void getBookingById_whenUserIdIsNotNumber_thenReturnBadRequest() {
        //given
        Long bookingId = 1L;
        String bookerId = "A";

        //when+then
        //в запросе передаем заголовок "X-Sharer-User-Id" из НЕ цифр
        mockMvc.perform(get("/bookings/{bookingId}", bookingId)
                        .param("X-Sharer-User-Id", bookerId))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getBookingById(anyLong(), anyLong());
    }

    @SneakyThrows
    @Test
    void getBookingById_whenBookingIdIsNotNumber_thenReturnBadRequest() {
        //given
        //в путь "/{bookingId}" передаем не цифру, а букву
        String bookingId = "A";
        Long bookerId = 1L;

        //when+then
        mockMvc.perform(get("/bookings/{bookingId}", bookingId)
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getBookingById(anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    void getAllBookingsForCurrentBooker_whenRequestValidAndPassedFromSizeState_thenReturnIsNotEmptyList() {
        //given
        Long bookerId = 1L;
        BookingStateFilter stateFilter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        when(bookingService.getListAllBookingsForCurrentUser(bookerId, stateFilter, from, size))
                .thenAnswer(i -> {
                    bookingResponse.setId(1L);

                    return List.of(bookingResponse);
                });

        //when+then
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", stateFilter.toString())
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].start").exists())
                .andExpect(jsonPath("$[0].end").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].booker.id").exists())
                .andExpect(jsonPath("$[0].booker.name").exists())
                .andExpect(jsonPath("$[0].booker.email").exists())
                .andExpect(jsonPath("$[0].item.id").exists())
                .andExpect(jsonPath("$[0].item.name").exists())
                .andExpect(jsonPath("$[0].item.description").exists());

        verify(bookingService).getListAllBookingsForCurrentUser(bookerId, stateFilter, from, size);
    }

    @Test
    @SneakyThrows
    void getListAllBookingsForCurrentBooker_whenRequestValidAndNotPassedFromAndSizeAndState_thenReturnIsNotEmptyList() {
        //given
        Long bookerId = 1L;

        // отсутствуют в запросе параметры from, size, state. Должны сработать значения по умолчанию в контроллерах
        when(bookingService.getListAllBookingsForCurrentUser(anyLong(), any(), anyInt(), anyInt()))
                .thenAnswer(i -> {
                    bookingResponse.setId(1L);

                    return List.of(bookingResponse);
                });

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].start").exists())
                .andExpect(jsonPath("$[0].end").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].booker.id").exists())
                .andExpect(jsonPath("$[0].booker.name").exists())
                .andExpect(jsonPath("$[0].booker.email").exists())
                .andExpect(jsonPath("$[0].item.id").exists())
                .andExpect(jsonPath("$[0].item.name").exists())
                .andExpect(jsonPath("$[0].item.description").exists());

        verify(bookingService).getListAllBookingsForCurrentUser(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    @SneakyThrows
    void getListAllBookingsForCurrentBooker_whenUserIdIsNull_thenBadRequest() {
        //given
        BookingStateFilter stateFilter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        //when+then
        // отсутствует заголовок X-Sharer-User-Id
        mockMvc.perform(get("/bookings")
                        .param("state", stateFilter.toString())
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getListAllBookingsForCurrentUser(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    @SneakyThrows
    void getListAllBookingsForCurrentBooker_whenUserIdNotNumber_thenBadRequest() {
        //given
        String bookerId = "A";
        BookingStateFilter stateFilter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        //when+then
        // заголовок X-Sharer-User-Id не число
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", stateFilter.toString())
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getListAllBookingsForCurrentUser(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    @SneakyThrows
    void getListAllBookingsForCurrentBooker_whenInvalidState_thenBadRequest() {
        //given
        Long bookerId = 1L;
        String stateFilter = "INVALID_STATE";
        int from = 0;
        int size = 10;

        //when+then
        // несуществующий state
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", stateFilter)
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: INVALID_STATE"));

        verify(bookingService, never()).getListAllBookingsForCurrentUser(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    @SneakyThrows
    void getListBookingsForCurrentOwner_whenRequestValidAndPassedFromSizeState_thenReturnIsNotEmptyList() {
        //given
        Long bookerId = 1L;
        BookingStateFilter stateFilter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        when(bookingService.getListAllBookingsForCurrentOwner(bookerId, stateFilter, from, size))
                .thenAnswer(i -> {
                    bookingResponse.setId(1L);

                    return List.of(bookingResponse);
                });

        //when+then
        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", stateFilter.toString())
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].start").exists())
                .andExpect(jsonPath("$[0].end").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].booker.id").exists())
                .andExpect(jsonPath("$[0].booker.name").exists())
                .andExpect(jsonPath("$[0].booker.email").exists())
                .andExpect(jsonPath("$[0].item.id").exists())
                .andExpect(jsonPath("$[0].item.name").exists())
                .andExpect(jsonPath("$[0].item.description").exists());

        verify(bookingService).getListAllBookingsForCurrentOwner(bookerId, stateFilter, from, size);
    }

    @Test
    @SneakyThrows
    void getListBookingsForCurrentOwner_whenRequestValidAndNotPassedFromAndSizeAndState_thenReturnIsNotEmptyList() {
        //given
        Long bookerId = 1L;

        // отсутствуют в запросе параметры from, size, state. Должны сработать значения по умолчанию в контроллерах
        when(bookingService.getListAllBookingsForCurrentOwner(anyLong(), any(), anyInt(), anyInt()))
                .thenAnswer(i -> {
                    bookingResponse.setId(1L);

                    return List.of(bookingResponse);
                });

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].start").exists())
                .andExpect(jsonPath("$[0].end").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].booker.id").exists())
                .andExpect(jsonPath("$[0].booker.name").exists())
                .andExpect(jsonPath("$[0].booker.email").exists())
                .andExpect(jsonPath("$[0].item.id").exists())
                .andExpect(jsonPath("$[0].item.name").exists())
                .andExpect(jsonPath("$[0].item.description").exists());

        verify(bookingService).getListAllBookingsForCurrentOwner(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    @SneakyThrows
    void getListBookingsForCurrentOwner_whenUserIdIsNull_thenBadRequest() {
        //given
        BookingStateFilter stateFilter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        //when+then
        // отсутствует заголовок X-Sharer-User-Id
        mockMvc.perform(get("/bookings/owner")
                        .param("state", stateFilter.toString())
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getListAllBookingsForCurrentOwner(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    @SneakyThrows
    void getListBookingsForCurrentOwner_whenUserIdNotNumber_thenBadRequest() {
        //given
        String bookerId = "A";
        BookingStateFilter stateFilter = BookingStateFilter.ALL;
        int from = 0;
        int size = 10;

        //when+then
        // заголовок X-Sharer-User-Id не число
        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", stateFilter.toString())
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getListAllBookingsForCurrentOwner(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    @SneakyThrows
    void getListBookingsForCurrentOwner_whenInvalidState_thenBadRequest() {
        //given
        Long bookerId = 1L;
        String stateFilter = "INVALID_STATE";
        int from = 0;
        int size = 10;

        //when+then
        // несуществующий state
        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", stateFilter)
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: INVALID_STATE"));

        verify(bookingService, never()).getListAllBookingsForCurrentOwner(anyLong(), any(), anyInt(), anyInt());
    }
}