package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.model.dto.ItemRequestDto;
import ru.practicum.shareit.request.storage.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;
import ru.practicum.shareit.util.MyPageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ItemRequestService itemRequestService;

    private ItemRequest itemRequest;

    private User requestor;

    @BeforeEach
    void setUp() {
        requestor = new User();
        requestor.setId(1L);

        itemRequest = new ItemRequest();
    }

    @Test
    void create_whenRequestValid_thenSaveItemRequest() {
        //given
        Long requestorId = requestor.getId();

        when(userService.existsUser(requestorId)).thenReturn(true);
        when(userService.getUserById(requestorId)).thenReturn(requestor);
        when(itemRequestRepository.save(itemRequest))
                .thenAnswer(i -> {
                    ItemRequest savedRequest = i.getArgument(0);
                    savedRequest.setId(1L);

                    return savedRequest;
                });

        //when
        ItemRequest result = itemRequestService.create(requestorId, itemRequest);

        //then
        //ожидаемые значения
        Long itemRequestIdExpected = 1L;
        User requestorExpected = requestor;

        assertEquals(requestorExpected, result.getRequestor());
        assertNotNull(result.getCreated());

        verify(userService).existsUser(requestorId);
        verify(userService).getUserById(requestorId);
        verify(itemRequestRepository).save(itemRequest);
    }

    @Test
    void create_whenUserNotFound_thenThrowResponseStatusException() {
        //given
        Long notExistRequestorId = 77L;

        when(userService.existsUser(notExistRequestorId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestService.create(notExistRequestorId, itemRequest));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найден пользователь по ID: " + 77 + " для создания запроса на вещь\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userService).existsUser(notExistRequestorId);
        verify(userService, never()).getUserById(anyLong());
        verify(itemRequestRepository, never()).save(any());
    }

    @Test
    void getItemRequestById_whenItemRequestExists_thenGetItemRequest() {
        //given
        Long requestId = 1L; //ID существующего запроса

        when(itemRequestRepository.findById(requestId)).thenReturn(Optional.of(itemRequest));

        //when
        ItemRequest result = itemRequestService.getItemRequestById(requestId);

        //then
        assertEquals(itemRequest, result);
        verify(itemRequestRepository).findById(requestId);
    }

    @Test
    void getItemRequestById_whenItemRequestNotExists_thenGetNull() {
        //given
        Long requestId = 1L; //ID несуществующего запроса

        when(itemRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        //when
        ItemRequest result = itemRequestService.getItemRequestById(requestId);

        //then
        assertNull(result);
        verify(itemRequestRepository).findById(requestId);
    }

    @Test
    void getItemRequestDtoById_whenItemRequestAndUserExists_thenGetItemRequestDto() {
        //given
        Long userId = requestor.getId();
        Long requestId = 1L;

        when(userService.existsUser(userId)).thenReturn(true);
        when(itemRequestRepository.findById(requestId))
                .thenAnswer(i -> {
                    Long itemRequestId = i.getArgument(0);

                    itemRequest.setDescription("Описание");
                    itemRequest.setId(itemRequestId);
                    itemRequest.setCreated(LocalDateTime.now());

                    return Optional.of(itemRequest);
                });

        //when
        ItemRequestDto result = itemRequestService.getItemRequestDtoById(requestId, userId);

        //then
        //ожидаемы значения
        Long itemRequestIdExpected = requestId;
        String descriptionExpected = "Описание";

        //проверка
        assertEquals(itemRequestIdExpected, result.getId());
        assertEquals(descriptionExpected, result.getDescription());
        assertNotNull(result.getCreated());
        assertNotNull(result.getItems());

        verify(userService).existsUser(userId);
        verify(itemRequestRepository).findById(requestId);
    }

    @Test
    void getItemRequestDtoById_whenItemUserNotExists_thenThrowResponseStatusException() {
        //given
        Long notExistUserId = requestor.getId(); //несуществующий пользователь
        Long requestId = 77L;

        when(userService.existsUser(notExistUserId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestService.getItemRequestDtoById(requestId, notExistUserId));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найден пользователь по ID: " + notExistUserId + ", для возвращения запроса на вещь по ID: " + requestId + "\"";

        //проверка
        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());

        verify(userService).existsUser(notExistUserId);
        verify(itemRequestRepository, never()).findById(anyLong());
    }

    @Test
    void getItemRequestDtoById_whenItemRequestNotExists_thenThrowResponseStatusException() {
        //given
        Long userId = requestor.getId();
        Long notExistRequestId = 77L; //несуществующий запрос вещи

        when(userService.existsUser(userId)).thenReturn(true);
        when(itemRequestRepository.findById(notExistRequestId)).thenReturn(Optional.empty());

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestService.getItemRequestDtoById(notExistRequestId, userId));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найден для возвращения по ID: " + notExistRequestId + " запрос на вещи, пользователем по ID: " + userId + "\"";

        //проверка
        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());

        verify(userService).existsUser(userId);
        verify(itemRequestRepository).findById(notExistRequestId);
    }

    @Test
    void getAllRequestByRequestorId_whenRequestorExists_thenGetAllRequest() {
        //given
        Long requestorId = requestor.getId();

        when(userService.existsUser(requestorId)).thenReturn(true);
        when(itemRequestRepository.findByRequestorId(requestorId))
                .thenAnswer(i ->
                {
                    itemRequest.setId(1L);
                    itemRequest.setDescription("Описание");
                    itemRequest.setCreated(LocalDateTime.now());
                    itemRequest.setRequestor(requestor);

                    return List.of(itemRequest);
                });
        //when
        List<ItemRequestDto> result = itemRequestService.getAllRequestByRequestorId(requestorId);

        //then
        //ожидаемые значения
        Long itemRequestIdExpected = 1L;
        String descriptionExpected = "Описание";

        //проверка
        ItemRequestDto resultDto = result.get(0);

        assertEquals(itemRequestIdExpected, resultDto.getId());
        assertEquals(descriptionExpected, resultDto.getDescription());
        assertNotNull(resultDto.getCreated());
        assertNotNull(resultDto.getItems());
        assertEquals(1, result.size());

        verify(userService).existsUser(requestorId);
        verify(itemRequestRepository).findByRequestorId(requestorId);
    }

    @Test
    void getAllRequestByRequestorId_whenRequestorNotExists_thenThrowResponseStatusException() {
        //given
        Long nonExistRequestorId = 77L; //несуществующий пользователь

        when(userService.existsUser(nonExistRequestorId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestService.getAllRequestByRequestorId(nonExistRequestorId));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найден пользователь по ID: " + nonExistRequestorId + " при возвращении его запросов на вещи\"";

        //проверка
        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());

        verify(userService).existsUser(nonExistRequestorId);
        verify(userService, never()).getUserById(anyLong());
        verify(itemRequestRepository, never()).findByRequestorId(any());
    }

    @Test
    void getAllItemRequest_whenUserExists_thenGetAllItemRequest() {
        //given
        int from = 0;
        int size = 10;
        Long requestorId = requestor.getId();

        when(userService.existsUser(requestorId)).thenReturn(true);
        when(itemRequestRepository.findByRequestorIdNot(any(), any()))
                .thenAnswer(i ->
                {
                    itemRequest.setId(1L);
                    itemRequest.setDescription("Описание");
                    itemRequest.setCreated(LocalDateTime.now());

                    List<ItemRequest> requests = List.of(itemRequest);

                    Page page = new PageImpl<>(requests);
                    return page;
                });

        //when
        List<ItemRequestDto> result = itemRequestService.getAllItemRequest(from, size, requestorId);

        //then
        //ожидаемые значения
        Long itemRequestIdExpected = 1L;
        String descriptionExpected = "Описание";

        //проверяем result
        ItemRequestDto resultDto = result.get(0);

        assertEquals(itemRequestIdExpected, resultDto.getId());
        assertEquals(descriptionExpected, resultDto.getDescription());
        assertNotNull(resultDto.getCreated());
        assertNotNull(resultDto.getItems());
        assertEquals(1, result.size());

        verify(userService).existsUser(requestorId);
        verify(itemRequestRepository).findByRequestorIdNot(any(), any());
    }

    @Test
    void getAllItemRequest_whenUserNotExists_thenThrowResponseStatusException() {
        //given
        int from = 0;
        int size = 10;
        Long notExistsRequestorId = 77L; //несуществующий пользователь

        when(userService.existsUser(notExistsRequestorId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemRequestService.getAllItemRequest(from, size, notExistsRequestorId));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найден пользователь по ID: " + notExistsRequestorId + " при возвращении всех запросов кроме его собственных запросов\"";

        //проверка
        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userService).existsUser(notExistsRequestorId);
        verify(userService, never()).getUserById(anyLong());
        verify(itemRequestRepository, never()).findByRequestorIdNot(any(), any());
    }
}