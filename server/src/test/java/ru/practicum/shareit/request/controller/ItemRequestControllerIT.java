package ru.practicum.shareit.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.model.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
public class ItemRequestControllerIT {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemRequestService itemRequestService;

    private final Long requestorId = 1L;

    private final Long requestId = 1L;

    private ItemRequestDto itemRequestDto;

    private ItemRequest itemRequest;

    @BeforeEach
    void setUp() {
        String description = "testDescription";
        LocalDateTime created = LocalDateTime.now().withNano(0);
        List<ItemDto> itemsDto = List.of(new ItemDto());
        List<Item> items = List.of(new Item());

        itemRequest = new ItemRequest();
        itemRequest.setDescription(description);
        itemRequest.setCreated(created);
        itemRequest.setItems(items);

        itemRequestDto = new ItemRequestDto();
        itemRequestDto.setDescription(description);
        itemRequestDto.setCreated(created);
        itemRequestDto.setItems(itemsDto);
    }

    @SneakyThrows
    @Test
    void create_whenValidRequest_thenReturnItemRequest() {
        //given
        when(itemRequestService.create(anyLong(), any()))
                .thenAnswer(i -> {

                    itemRequest.setId(requestId);

                    return itemRequest;
                });

        //when+then
        String responseJson = mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", requestorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //полученный JSON переводим в объект ItemRequest
        ItemRequest jsonToItemRequest = objectMapper.readValue(responseJson, ItemRequest.class);

        //ожидаемые значения
        Long idExpected = requestId;
        String descriptionExpected = itemRequest.getDescription();
        LocalDateTime created = itemRequest.getCreated();

        //проверка
        assertEquals(idExpected, jsonToItemRequest.getId());
        assertEquals(descriptionExpected, jsonToItemRequest.getDescription());
        assertEquals(created, jsonToItemRequest.getCreated());
        assertEquals(1, jsonToItemRequest.getItems().size());

        verify(itemRequestService).create(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void create_whenItemRequestFieldsEmpty_thenReturnBadRequest() {
        //given
        ItemRequest newItemRequest = new ItemRequest();

        //when+then
        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", requestorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItemRequest)))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).create(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void create_whenBodyEmpty_thenReturnBadRequest() {

        //when+then
        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", requestorId))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).create(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void create_whenUserIdEmpty_thenReturnBadRequest() {

        //when+then
        //в запросе отсутствует заголовок "X-Sharer-User-Id"
        mockMvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest)))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).create(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void create_whenUserIdIsNotNumber_thenReturnBadRequest() {
        //given
        String userId = "A";

        //when+then
        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).create(anyLong(), any());
    }

    @SneakyThrows
    @Test
    void getItemRequestById_whenValidRequest_thenReturnItemRequest() {
        //given
        when(itemRequestService.getItemRequestDtoById(requestorId, requestId))
                .thenAnswer(i -> {

                    itemRequestDto.setId(requestId);

                    return itemRequestDto;
                });

        //when+then
        String responseJson = mockMvc.perform(get("/requests/{requestId}", requestId)
                        .header("X-Sharer-User-Id", requestorId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //полученный JSON переводим в объект ItemRequest
        ItemRequest jsonToItemRequest = objectMapper.readValue(responseJson, ItemRequest.class);

        //ожидаемые значения
        Long idExpected = requestId;
        String descriptionExpected = itemRequest.getDescription();
        LocalDateTime created = itemRequest.getCreated();

        //проверка
        assertEquals(idExpected, jsonToItemRequest.getId());
        assertEquals(descriptionExpected, jsonToItemRequest.getDescription());
        assertEquals(created, jsonToItemRequest.getCreated());
        assertEquals(1, jsonToItemRequest.getItems().size());

        verify(itemRequestService).getItemRequestDtoById(requestorId, requestId);
    }

    @SneakyThrows
    @Test
    void getItemRequestById_whenUserIdEmpty_thenReturnItemRequest() {

        //when+then
        //в запросе отсутствует заголовок "X-Sharer-User-Id"
        mockMvc.perform(get("/requests/{requestId}", requestId))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getItemRequestDtoById(requestorId, requestId);
    }

    @SneakyThrows
    @Test
    void getAllRequestByRequestorId_whenValidRequest_thenReturnNotEmptyList() {
        //given
        when(itemRequestService.getAllRequestByRequestorId(requestorId))
                .thenAnswer(i -> {

                    itemRequestDto.setId(requestId);

                    List<ItemRequestDto> items = List.of(itemRequestDto);

                    return items;
                });

        //when+then
        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", requestorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].created").exists())
                .andExpect(jsonPath("$[0].items").exists());

        verify(itemRequestService).getAllRequestByRequestorId(requestorId);
    }

    @SneakyThrows
    @Test
    void getAllRequestByRequestorId_whenUserIdIsNull_thenReturnBadRequest() {
        //when+then
        //в запросе отсутствует заголовок "X-Sharer-User-Id"
        mockMvc.perform(get("/requests"))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getAllRequestByRequestorId(requestorId);
    }

    @SneakyThrows
    @Test
    void getAllRequestByRequestorId_whenUserIdIsNotNumber_thenReturnBadRequest() {
        //given
        String userId = "A";

        //when+then
        //заголовок "X-Sharer-User-Id" не цифра
        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getAllRequestByRequestorId(anyLong());
    }

    @SneakyThrows
    @Test
    void getAllItemRequest_whenValidRequest_thenReturnNotEmptyList() {
        //given
        int from = 0;
        int size = 10;

        when(itemRequestService.getAllItemRequest(from, size, requestorId))
                .thenAnswer(i -> {

                    itemRequestDto.setId(requestId);

                    List<ItemRequestDto> items = List.of(itemRequestDto);

                    return items;
                });

        //when+then
        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", requestorId)
                        .param("from", String.valueOf(from))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].created").exists())
                .andExpect(jsonPath("$[0].items").exists());

        verify(itemRequestService).getAllItemRequest(from, size, requestorId);
    }

    @SneakyThrows
    @Test
    void getAllItemRequest_whenRequestValidAndNotPassedFromAndSize_thenReturnIsNotEmptyList() {
        //given
        when(itemRequestService.getAllItemRequest(anyInt(), anyInt(), anyLong()))
                .thenAnswer(i -> {

                    itemRequestDto.setId(requestId);

                    List<ItemRequestDto> items = List.of(itemRequestDto);

                    return items;
                });

        //when+then
        // отсутствуют в запросе параметры from, size. Должны сработать значения по умолчанию в контроллерах
        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", requestorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].created").exists())
                .andExpect(jsonPath("$[0].items").exists());

        verify(itemRequestService).getAllItemRequest(anyInt(), anyInt(), anyLong());
    }

    @SneakyThrows
    @Test
    void getAllItemRequest_whenUserIdIsNull_thenReturnBadRequest() {
        //given
        int from = 0;
        int size = 10;

        //when+then
        //в запросе отсутствует заголовок "X-Sharer-User-Id"
        mockMvc.perform(get("/requests/all")
                        .param("from", String.valueOf(from))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getAllItemRequest(anyInt(), anyInt(), anyLong());
    }

    @SneakyThrows
    @Test
    void getAllItemRequest_whenUserIdIsNotNumber_thenReturnIsNotEmptyList() {
        //given
        int from = 0;
        int size = 10;
        String userId = "A";

        //when+then
        //заголовок "X-Sharer-User-Id" не цифра
        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", userId)
                        .param("from", String.valueOf(from))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getAllItemRequest(anyInt(), anyInt(), anyLong());
    }
}