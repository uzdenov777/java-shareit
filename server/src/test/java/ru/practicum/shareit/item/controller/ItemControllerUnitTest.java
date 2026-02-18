package ru.practicum.shareit.item.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.item.model.dto.CommentRequest;
import ru.practicum.shareit.item.model.dto.CommentResponse;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.item.model.dto.ItemResponse;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemControllerUnitTest {

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ItemController itemController;

    private Long userId = 1L;

    private ItemDto itemDto;
    private ItemResponse itemResponse;
    private CommentRequest commentRequest;

    @BeforeEach
    void setUp() {
        itemDto = new ItemDto();

        itemResponse = new ItemResponse();

        commentRequest = new CommentRequest();
        commentRequest.setText("TestText");
    }

    @Test
    void add_whenRequestValid_thenSaveItem() {
        //given
        when(itemService.add(userId, itemDto)).thenReturn(itemResponse);

        //when
        ItemResponse savedItem = itemController.add(userId, itemDto);

        //then
        assertEquals(itemResponse, savedItem);
        verify(itemService).add(userId, itemDto);
    }

    @Test
    void add_whenUserNotExist_thenThrowResponseStatusException() {
        //given
        Long notExistUserId = 777L;
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "Пользователь по ID: " + notExistUserId + " не найден для сохранения вещи: " + itemDto;

        ResponseStatusException exception = new ResponseStatusException(status, exceptionMessage);

        when(itemService.add(notExistUserId, itemDto)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemController.add(notExistUserId, itemDto));

        //then
        assertEquals(exception, resException);
        verify(itemService).add(notExistUserId, itemDto);
    }

    @Test
    void updateItem_whenRequestValid_thenUpdateItem() {
        //given
        ItemResponse newVersion = new ItemResponse();
        newVersion.setName("newName");
        newVersion.setDescription("newDescription");

        Long itemId = 1L; //ID уже сохраненной вещи для обновления

        when(itemService.updateItem(userId, itemId, itemDto)).thenReturn(newVersion);

        //when
        ItemResponse savedItem = itemController.updateItem(userId, itemId, itemDto);

        //then
        assertEquals(newVersion, savedItem);
        verify(itemService).updateItem(userId, itemId, itemDto);
    }

    @Test
    void updateItem_whenNotExist_thenThrowResponseStatusException() {
        //given
        Long nonExistItemId = 1L; //ID уже сохраненной вещи для обновления

        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "Вещь для обновления не найдена по ID: " + nonExistItemId;

        ResponseStatusException exception = new ResponseStatusException(status, exceptionMessage);

        when(itemService.updateItem(userId, nonExistItemId, itemDto)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemController.updateItem(userId, nonExistItemId, itemDto));

        //then
        assertEquals(exception, resException);
        verify(itemService).updateItem(userId, nonExistItemId, itemDto);
    }

    @Test
    void updateItem_whenUserNotExist_thenThrowResponseStatusException() {
        //given
        Long nonExistUserId = 777L; //несуществующий пользователь
        Long itemId = 1L; //ID уже сохраненной вещи для обновления

        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "Владелец вещи по ID:" + nonExistUserId + " не найден в базе данных при обновлении вещи по ID:" + itemId;

        ResponseStatusException exception = new ResponseStatusException(status, exceptionMessage);

        when(itemService.updateItem(nonExistUserId, itemId, itemDto)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemController.updateItem(nonExistUserId, itemId, itemDto));

        //then
        assertEquals(exception, resException);
        verify(itemService).updateItem(nonExistUserId, itemId, itemDto);
    }

    @Test
    void updateItem_whenUserNotOwnerItem_thenThrowResponseStatusException() {
        //given
        Long itemId = 1L; //ID уже сохраненной вещи для обновления

        HttpStatus status = HttpStatus.FORBIDDEN;
        String exceptionMessage = "Пользователь по ID:" + userId + " не владелец вещи по ID:" + itemId;

        ResponseStatusException exception = new ResponseStatusException(status, exceptionMessage);

        when(itemService.updateItem(userId, itemId, itemDto)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemController.updateItem(userId, itemId, itemDto));

        //then
        assertEquals(exception, resException);
        verify(itemService).updateItem(userId, itemId, itemDto);
    }

    @Test
    void getItemById_whenRequestValid_thenGetItemById() {
        //given
        Long itemId = 1L; //ID уже сохраненной вещи для обновления

        when(itemService.getItemResponseByIdFromUser(userId, itemId)).thenReturn(itemResponse);

        //when
        ItemResponse res = itemController.getItemById(userId, itemId);

        //then
        assertEquals(itemResponse, res);
        verify(itemService).getItemResponseByIdFromUser(userId, itemId);
    }

    @Test
    void getItemById_whenUserNotFound_thenThrowResponseStatusException() {
        //given
        Long itemId = 1L; //ID уже сохраненной вещи для обновления
        Long nonExistUserId = 1L; //несуществующий пользователь

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь по ID: " + itemId);

        when(itemService.getItemResponseByIdFromUser(nonExistUserId, itemId)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemController.getItemById(nonExistUserId, itemId));

        //then
        assertEquals(exception, resException);
        verify(itemService).getItemResponseByIdFromUser(nonExistUserId, itemId);
    }

    @Test
    void getItemById_whenItemNotFound_thenThrowResponseStatusException() {
        //given
        Long nonExistItemId = 777L; //несуществующая вещь

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдена вещь по ID: " + nonExistItemId);

        when(itemService.getItemResponseByIdFromUser(userId, nonExistItemId)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemController.getItemById(userId, nonExistItemId));

        //then
        assertEquals(exception, resException);
        verify(itemService).getItemResponseByIdFromUser(userId, nonExistItemId);
    }

    @Test
    void getAllItemsFromUser_whenRequestValid_thenReturnAllItemsFromUser() {
        //given
        int from = 0;
        int size = 10;

        List<ItemResponse> items = List.of(itemResponse);

        when(itemService.getAllItemsFromUser(userId, from, size)).thenReturn(items);

        //when
        List<ItemResponse> res = itemController.getAllItemsFromUser(userId, from, size);

        //then
        assertEquals(items, res);
        verify(itemService).getAllItemsFromUser(userId, from, size);
    }

    @Test
    void getAllItemsFromUser_whenUserNotFound_thenThrowResponseStatusException() {
        //given
        int from = 0;
        int size = 10;
        long nonExistUserId = 777L; //Несуществующий пользователь

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещей по ID:" + nonExistUserId + " не найден в базе данных для возврате всех его вещей");

        when(itemService.getAllItemsFromUser(userId, from, size)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemController.getAllItemsFromUser(userId, from, size));

        //then
        assertEquals(exception, resException);
        verify(itemService).getAllItemsFromUser(userId, from, size);
    }

    @Test
    void searchItem_whenRequestValid_thenReturnListIsNotEmpty() {
        //given
        List<ItemResponse> items = List.of(itemResponse);

        //параметры для запроса
        int from = 0;
        int size = 10;
        String text = "Какой-то поиск";

        when(itemService.itemSearch(text, userId, from, size)).thenReturn(items);

        //when
        List<ItemResponse> res = itemController.searchItem(text, userId, from, size);

        //then
        assertEquals(items, res);
        verify(itemService).itemSearch(text, userId, from, size);
    }

    @Test
    void searchItem_whenUserNotFound_thenReturnListIsEmpty() {
        //given
        List<ItemResponse> items = List.of();// должен вернуть пустой список

        //параметры для запроса
        int from = 0;
        int size = 10;
        String text = "Какой-то поиск";
        Long nonExistUserId = 777L;

        when(itemService.itemSearch(text, nonExistUserId, from, size)).thenReturn(items);

        //when
        List<ItemResponse> res = itemController.searchItem(text, nonExistUserId, from, size);

        //then
        assertEquals(items, res);
        verify(itemService).itemSearch(text, nonExistUserId, from, size);
    }

    @Test
    void searchItem_whenTextBlank_thenReturnListIsEmpty() {
        //given
        List<ItemResponse> items = List.of();// должен вернуть пустой список

        //параметры для запроса
        int from = 0;
        int size = 10;
        String text = ""; //пустой или null

        when(itemService.itemSearch(text, userId, from, size)).thenReturn(items);

        //when
        List<ItemResponse> res = itemController.searchItem(text, userId, from, size);

        //then
        assertEquals(items, res);
        verify(itemService).itemSearch(text, userId, from, size);
    }

    @Test
    void addComment_whenRequestValid_thenAddingCommentAndReturnResponseIsCode200() {
        //given
        Long itemId = 1L; //ID уже сохраненной вещи для обновления
        String textComment = commentRequest.getText();

        CommentResponse commentResponse = new CommentResponse();
        commentResponse.setText(textComment);

        when(itemService.addComment(userId, itemId, textComment)).thenReturn(commentResponse);

        //when
        CommentResponse response = itemController.addComment(userId, itemId, commentRequest);

        //then
        assertEquals(commentResponse, response);
        verify(itemService).addComment(userId, itemId, textComment);
    }

    @Test
    void addComment_whenCheckUserRentalIsFalse_thenThrowResponseStatusException() {
        //given
        Long itemId = 1L;
        String textComment = commentRequest.getText();

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Пользователь по ID: " + userId + " не имеет право добавить комментарий вещи по ID: " + itemId + ", так как не брал и не завершил аренду этого предмета");

        when(itemService.addComment(userId, itemId, textComment)).thenThrow(exception);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemController.addComment(userId, itemId, commentRequest));

        //then
        assertEquals(exception, resException);
        verify(itemService).addComment(userId, itemId, textComment);
    }
}