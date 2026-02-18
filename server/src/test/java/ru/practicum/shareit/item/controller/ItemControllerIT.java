package ru.practicum.shareit.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.model.dto.CommentRequest;
import ru.practicum.shareit.item.model.dto.CommentResponse;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.item.model.dto.ItemResponse;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerIT {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    private ItemDto itemDto;

    private ItemResponse itemResponse;

    private CommentRequest commentRequest;

    private CommentResponse commentResponse;

    private final Long ownerId = 1L;

    private final Long itemId = 1L;

    @BeforeEach
    void setUp() {
        String name = "itemName";
        String description = "itemDescription";
        boolean available = true;

        itemDto = new ItemDto();
        itemDto.setName(name);
        itemDto.setDescription(description);
        itemDto.setAvailable(available);

        itemResponse = new ItemResponse();
        itemResponse.setName(name);
        itemResponse.setDescription(description);
        itemResponse.setAvailable(available);

        commentRequest = new CommentRequest();
        commentRequest.setText("commentText");

        commentResponse = new CommentResponse();
        commentResponse.setText(commentRequest.getText());
        commentResponse.setAuthorName("authorName");
        commentResponse.setCreated(LocalDateTime.now());
    }

    @SneakyThrows
    @Test
    void add_whenValidRequest_thenAddingAndReturnItemResponse() {
        //given
        when(itemService.add(ownerId, itemDto))
                .thenAnswer(i -> {

                    itemResponse.setId(1L);

                    return itemResponse;
                });

        //when+then
        String responseJson = mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.available").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();


        //полученный JSON переводим в объект ItemResponse
        ItemResponse jsonToItemResponse = objectMapper.readValue(responseJson, ItemResponse.class);

        //ожидаемые значения
        Long itemIdExpected = itemResponse.getId();
        String nameExpected = itemResponse.getName();
        String descriptionExpected = itemResponse.getDescription();
        boolean availableExpected = itemResponse.getAvailable();

        //проверка
        assertEquals(itemIdExpected, jsonToItemResponse.getId());
        assertEquals(nameExpected, jsonToItemResponse.getName());
        assertEquals(descriptionExpected, jsonToItemResponse.getDescription());
        assertEquals(availableExpected, jsonToItemResponse.getAvailable());

        verify(itemService).add(ownerId, itemDto);
    }

    @SneakyThrows
    @Test
    void add_whenUserIdIsNull_thenReturnBadRequest() {

        //не передаем в запросе "X-Sharer-User-Id" == userId = null
        //when+then
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());  // ожидаем 400

        verify(itemService, never()).add(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void add_whenUserIdIsNotNumber_thenReturnBadRequest() {
        //given
        String notNumber = "notNumber";

        // передаем в запросе "X-Sharer-User-Id" не цифру
        //when+then
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", notNumber) //передали в заголовок не цифру
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).add(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void add_whenEmptyItemDto_thenReturnBadRequest() {

        //не передаем тело в запросе = body == null
        //when+then
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isBadRequest());  // ожидаем 400

        verify(itemService, never()).add(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void add_whenItemDtoFieldsEmpty_thenReturnBadRequest() {
        //given
        //не заполняем поля
        ItemDto newItemDto = new ItemDto();

        //when+then
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItemDto))
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isBadRequest());  // ожидаем 400

        verify(itemService, never()).add(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void updateItem_whenValidRequest_thenUpdatingAndReturnItemResponse() {
        //given
        when(itemService.updateItem(ownerId, itemId, itemDto))
                .thenAnswer(i -> {

                    itemResponse.setId(itemId);

                    return itemResponse;
                });

        //when+then
        String responseJson = mockMvc.perform(patch("/items/{itemId}", itemId)
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.available").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();


        //полученный JSON переводим в объект ItemResponse
        ItemResponse jsonToItemResponse = objectMapper.readValue(responseJson, ItemResponse.class);

        //ожидаемые значения
        Long itemIdExpected = itemResponse.getId();
        String nameExpected = itemResponse.getName();
        String descriptionExpected = itemResponse.getDescription();
        boolean availableExpected = itemResponse.getAvailable();

        //проверка
        assertEquals(itemIdExpected, jsonToItemResponse.getId());
        assertEquals(nameExpected, jsonToItemResponse.getName());
        assertEquals(descriptionExpected, jsonToItemResponse.getDescription());
        assertEquals(availableExpected, jsonToItemResponse.getAvailable());

        verify(itemService).updateItem(ownerId, itemId, itemDto);
    }

    @SneakyThrows
    @Test
    void updateItem_whenUserIdIsNull_thenReturnBadRequest() {

        //не передаем в запросе "X-Sharer-User-Id" == userId = null, но передаем в пути itemId и в теле itemDto
        //when+then
        mockMvc.perform(patch("/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());  // ожидаем 400

        verify(itemService, never()).updateItem(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void updateItem_whenUserIdIsNotNumber_thenReturnBadRequest() {
        //given
        String notNumber = "notNumber";

        // передаем в запросе "X-Sharer-User-Id" не цифру, но передаем в пути itemId и в теле itemDto
        //when+then
        mockMvc.perform(patch("/items/{itemId}", itemId)
                        .header("X-Sharer-User-Id", notNumber) //передали в заголовок не цифру
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).updateItem(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void updateItem_whenItemIdIsNotNumber_thenReturnBadRequest() {
        //given
        String notNumber = "notNumber";

        //when+then
        mockMvc.perform(patch("/items/{itemId}", notNumber)// передаем в пути запроса itemId не цифру
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemResponse)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).updateItem(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void updateItem_whenEmptyBody_thenReturnBadRequest() {

        //не передаем тело в запросе = body == null
        //when+then
        mockMvc.perform(patch("/items/{itemId}", itemId)
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isBadRequest());  // ожидаем 400

        verify(itemService, never()).updateItem(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void updateItem_whenItemDtoFieldsEmpty_thenReturnBadRequest() {
        //given
        //не заполняем поля
        ItemDto newItemDto = new ItemDto();

        //when+then
        mockMvc.perform(patch("/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItemDto))
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isOk());  // ожидаем 200

        verify(itemService).updateItem(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void getItemById_whenRequestValid_thenReturnItemResponse() {

        //given
        when(itemService.getItemResponseByIdFromUser(ownerId, itemId))
                .thenAnswer(i ->
                {
                    itemResponse.setId(itemId);
                    return itemResponse;
                });

        //when+then
        String responseJson = mockMvc.perform(get("/items/{itemId}", itemId)
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.available").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();


        //полученный JSON переводим в объект ItemResponse
        ItemResponse jsonToItemResponse = objectMapper.readValue(responseJson, ItemResponse.class);

        //ожидаемые значения
        Long itemIdExpected = itemResponse.getId();
        String nameExpected = itemResponse.getName();
        String descriptionExpected = itemResponse.getDescription();
        boolean availableExpected = itemResponse.getAvailable();

        //проверка
        assertEquals(itemIdExpected, jsonToItemResponse.getId());
        assertEquals(nameExpected, jsonToItemResponse.getName());
        assertEquals(descriptionExpected, jsonToItemResponse.getDescription());
        assertEquals(availableExpected, jsonToItemResponse.getAvailable());

        verify(itemService).getItemResponseByIdFromUser(ownerId, itemId);
    }

    @SneakyThrows
    @Test
    void getItemById_whenUserIdIsNull_thenReturnBadRequest() {
        //when+then
        //в запросе не передаем заголовок "X-Sharer-User-Id" == userId = Null
        mockMvc.perform(get("/items/{itemId}", itemId))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).getItemResponseByIdFromUser(anyLong(), anyLong());
    }

    @SneakyThrows
    @Test
    void getItemById_whenUserIdIsNotNumber_thenReturnBadRequest() {
        //given
        String userId = "A";

        //when+then
        //в запросе передаем заголовок "X-Sharer-User-Id" из НЕ цифр
        mockMvc.perform(get("/items/{itemId}", itemId)
                        .param("X-Sharer-User-Id", userId))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).getItemResponseByIdFromUser(anyLong(), anyLong());
    }

    @SneakyThrows
    @Test
    void getItemById_whenItemIdIsNotNumber_thenReturnBadRequest() {
        //given
        //в путь "/{itemId}" передаем не цифру, а букву
        String itemId = "A";

        //when+then
        mockMvc.perform(get("/items/{itemId}", itemId)
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).getItemResponseByIdFromUser(anyLong(), anyLong());
    }

    @SneakyThrows
    @Test
    void getAllItemsFromUser_whenRequestValid_thenReturnIsNotEmptyList() {
        //given
        int from = 0;
        int size = 10;

        when(itemService.getAllItemsFromUser(ownerId, from, size))
                .thenAnswer(i -> {
                    itemResponse.setId(itemId);

                    return List.of(itemResponse);
                });

        //when+then
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].available").exists());

        verify(itemService).getAllItemsFromUser(ownerId, from, size);
    }

    @SneakyThrows
    @Test
    void getAllItemsFromUser_whenRequestValidAndNotPassedFromAndSize_thenReturnIsNotEmptyList() {
        //given
        when(itemService.getAllItemsFromUser(anyLong(), anyInt(), anyInt()))
                .thenAnswer(i -> {
                    itemResponse.setId(itemId);

                    return List.of(itemResponse);
                });

        //when+then
        // отсутствуют в запросе параметры from, size. Должны сработать значения по умолчанию в контроллерах
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].available").exists());

        verify(itemService).getAllItemsFromUser(anyLong(), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void getAllItemsFromUser_whenUserIdIsNull_thenBadRequest() {
        //given
        int from = 0;
        int size = 10;

        //when+then
        //в запросе не передаем заголовок "X-Sharer-User-Id" == userId = Null
        mockMvc.perform(get("/items")
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).getAllItemsFromUser(anyLong(), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void getAllItemsFromUser_whenUserIdIsNotNumber_thenBadRequest() {
        //given
        int from = 0;
        int size = 10;
        String userId = "A";

        //when+then
        //в запросе передаем заголовок "X-Sharer-User-Id" из НЕ цифр
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", userId)
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).getAllItemsFromUser(anyLong(), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void searchItem_whenRequestValid_thenReturnIsNotEmptyList() {
        //given
        String text = "A";
        int from = 0;
        int size = 10;

        when(itemService.itemSearch(text, ownerId, from, size))
                .thenAnswer(i -> {

                    itemResponse.setId(itemId);
                    itemResponse.setName(text);

                    return List.of(itemResponse);
                });

        //when+then
        mockMvc.perform(get("/items/search")
                        .param("text", text)
                        .header("X-Sharer-User-Id", ownerId)
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].available").exists());

        verify(itemService).itemSearch(text, ownerId, from, size);
    }

    @SneakyThrows
    @Test
    void searchItem_whenRequestValidAndNotPassedFromAndSizeAndText_thenReturnIsNotEmptyList() {
        //given
        when(itemService.itemSearch(anyString(), anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());

        //when+then
        // отсутствуют в запросе параметры text, from, size. Должны сработать значения по умолчанию в контроллерах
        mockMvc.perform(get("/items/search")
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(itemService).itemSearch(anyString(), anyLong(), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void searchItem_whenUserIdIsNull_thenReturnEmptyList() {
        //given
        String text = "A";
        int from = 0;
        int size = 10;

        //when+then
        // отсутствует заголовок X-Sharer-User-Id
        mockMvc.perform(get("/items/search")
                        .param("text", text)
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).itemSearch(anyString(), anyLong(), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void searchItem_whenUserIdNotNumber_thenReturnEmptyList() {
        //given
        String text = "A";
        String userId = "userId";
        int from = 0;
        int size = 10;

        //when+then
        // отсутствует заголовок X-Sharer-User-Id
        mockMvc.perform(get("/items/search")
                        .param("text", text)
                        .header("X-Sharer-User-Id", userId)
                        .param("from", Integer.toString(from))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).itemSearch(anyString(), anyLong(), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void addComment_whenRequestValid_thenReturnCommentResponse() {
        //given
        when(itemService.addComment(anyLong(), anyLong(), any()))
                .thenAnswer(i -> {

                    commentResponse.setId(1L);

                    return commentResponse;
                });

        //when+then
        String responseJson = mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.text").exists())
                .andExpect(jsonPath("$.authorName").exists())
                .andExpect(jsonPath("$.created").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //полученный JSON переводим в объект CommentResponse
        CommentResponse jsonToCommentResponse = objectMapper.readValue(responseJson, CommentResponse.class);

        //ожидаемые значения
        Long commentIdExpected = commentResponse.getId();
        String commentTextExpected = commentResponse.getText();
        String commentAuthorNameExpected = commentResponse.getAuthorName();
        LocalDateTime commentCreatedExpected = commentResponse.getCreated();

        //проверка
        assertEquals(commentIdExpected, jsonToCommentResponse.getId());
        assertEquals(commentTextExpected, jsonToCommentResponse.getText());
        assertEquals(commentAuthorNameExpected, jsonToCommentResponse.getAuthorName());
        assertEquals(commentCreatedExpected, jsonToCommentResponse.getCreated());

        verify(itemService).addComment(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void addComment_whenCommentTextEmpty_thenReturnBadRequest() {
        //given
        //текст пустой
        commentRequest.setText("");

        //when+then
        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).addComment(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void addComment_whenBodyIsNull_thenReturnBadRequest() {
        //given
        //текст пустой
        commentRequest = null;

        //when+then
        // передали в теле запроса Null
        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).addComment(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void addComment_whenUserIdIsNull_thenReturnBadRequest() {
        //when+then
        //не передаем в запросе заголовок "X-Sharer-User-Id" == userId = null
        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).addComment(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void addComment_whenUserIdIsNotNumber_thenReturnBadRequest() {
        //given
        String userId = "userId";

        //when+then
        // передаем в запросе "X-Sharer-User-Id" не цифру
        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header("X-Sharer-User-Id", userId) //передали в заголовок не цифру
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).addComment(anyLong(), anyLong(), any());
    }

    @SneakyThrows
    @Test
    void addComment_whenItemIdIsNotNumber_thenReturnBadRequest() {
        //given
        String itemId = "Id";

        //when+then
        //передаем в пути запроса {itemId} НЕ цифру
        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).addComment(anyLong(), anyLong(), any());
    }
}