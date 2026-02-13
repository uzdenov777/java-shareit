package ru.practicum.shareit.request.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.model.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestControllerUnitTest {

    @Mock
    private ItemRequestService itemRequestService;

    @InjectMocks
    private ItemRequestController itemRequestController;

    private ItemRequest itemRequest;

    private User requester;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);

        itemRequest = new ItemRequest();
    }

    @Test
    void create_whenRequestorFound_thenSaveItemRequest() {
        //given
        Long requestorId = requester.getId();

        when(itemRequestService.create(requestorId, itemRequest))
                .thenAnswer(i ->
                {
                    ItemRequest itemRequest = i.getArgument(1);

                    itemRequest.setId(1L);
                    itemRequest.setRequestor(requester);

                    return itemRequest;
                });

        //when
        ItemRequest saved = itemRequestController.create(requestorId, itemRequest);

        //then
        //ожидаемы значения
        Long itemRequestIdExpected = 1L;
        User requesterExpected = requester;

        assertEquals(itemRequestIdExpected, saved.getId());
        assertEquals(requesterExpected, saved.getRequestor());

        verify(itemRequestService).create(requestorId, itemRequest);
    }

    @Test
    void create_whenRequestorNotFound_thenThrowResponseStatusException() {
        //given
        Long notExitsRequestorId = 77L; //ID не существующего пользователя

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь по ID: " + notExitsRequestorId + " для создания запроса на вещь");

        when(itemRequestService.create(notExitsRequestorId, itemRequest))
                .thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestController.create(notExitsRequestorId, itemRequest));

        //then
        assertEquals(exception, resException);
        verify(itemRequestService).create(notExitsRequestorId, itemRequest);
    }

    @Test
    void getByRequestorId_whenRequestorFound_thenGetItemRequest() {
        //given
        List<ItemRequestDto> requests = List.of(new ItemRequestDto());

        Long requestorId = requester.getId();

        when(itemRequestService.getAllRequestByRequestorId(requestorId)).thenReturn(requests);

        //when
        List<ItemRequestDto> result = itemRequestController.getAllRequestByRequestorId(requestorId);

        //then
        assertEquals(requests, result);
        verify(itemRequestService).getAllRequestByRequestorId(requestorId);
    }

    @Test
    void getByRequestorId_whenRequestorNotFound_thenThrowResponseStatusException() {
        //given
        Long notExitsRequestorId = 77L; //несуществующий пользователь

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь по ID: " + notExitsRequestorId + " при возвращении его запросов на вещи");

        when(itemRequestService.getAllRequestByRequestorId(notExitsRequestorId)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestController.getAllRequestByRequestorId(notExitsRequestorId));

        //then
        assertEquals(exception, resException);
        verify(itemRequestService).getAllRequestByRequestorId(notExitsRequestorId);
    }

    @Test
    void getAllItemRequest_whenUserFound_thenGetAllItemRequestExceptForUserOwnRequests() {
        //given
        List<ItemRequestDto> requests = List.of(new ItemRequestDto());

        int from = 0;
        int size = 10;
        Long requestorId = requester.getId();

        when(itemRequestService.getAllItemRequest(from, size, requestorId)).thenReturn(requests);

        //when
        List<ItemRequestDto> result = itemRequestController.getAllItemRequest(from, size, requestorId);

        //then
        assertEquals(requests, result);
        verify(itemRequestService).getAllItemRequest(from, size, requestorId);
    }

    @Test
    void getAllItemRequest_whenUserNotFound_thenThrowResponseStatusException() {
        //given
        int from = 0;
        int size = 10;
        Long notExitsRequestorId = 77L;

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь по ID: " + notExitsRequestorId + " при возвращении его запросов на вещи");

        when(itemRequestService.getAllItemRequest(from, size, notExitsRequestorId)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestService.getAllItemRequest(from, size, notExitsRequestorId));

        //then
        assertEquals(exception, resException);
        verify(itemRequestService).getAllItemRequest(from, size, notExitsRequestorId);
    }

    @Test
    void getItemRequestDtoById_whenUserAndItemRequestFound_thenGetItemRequest() {
        //given
        ItemRequestDto itemRequestDto = new ItemRequestDto();
        itemRequestDto.setId(1L);

        Long requestorId = requester.getId();
        Long itemRequestId = itemRequestDto.getId();

        when(itemRequestService.getItemRequestDtoById(itemRequestId, requestorId)).thenReturn(itemRequestDto);

        //when
        ItemRequestDto result = itemRequestController.getItemRequestById(requestorId, itemRequestId);

        //then
        assertEquals(itemRequestDto, result);
        verify(itemRequestService).getItemRequestDtoById(itemRequestId, requestorId);
    }


    @Test
    void getItemRequestDtoById_whenItemRequestNotFound_thenThrowResponseStatusException() {
        //given
        Long requestorId = requester.getId();
        Long notExitsRequestId = 77L; //несуществующий запрос

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден для возвращения по ID: " + notExitsRequestId + " запрос на вещи, пользователем по ID: " + requestorId);

        when(itemRequestService.getItemRequestDtoById(notExitsRequestId, requestorId)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestController.getItemRequestById(requestorId, notExitsRequestId));

        //then
        assertEquals(exception, resException);
        verify(itemRequestService).getItemRequestDtoById(notExitsRequestId, requestorId);
    }

    @Test
    void getItemRequestDtoById_whenUserNotFound_thenThrowResponseStatusException() {
        //given
        Long notExitsRequestorId = 77L; //несуществующий пользователь
        Long requestId = 1L;

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь по ID: " + notExitsRequestorId + ", для возвращения запроса на вещь по ID: " + requestId);

        when(itemRequestService.getItemRequestDtoById(requestId, notExitsRequestorId)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestController.getItemRequestById(notExitsRequestorId, requestId));

        //then
        assertEquals(exception, resException);
        verify(itemRequestService).getItemRequestDtoById(requestId, notExitsRequestorId);
    }
}