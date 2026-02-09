package ru.practicum.shareit.request.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.model.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

/**
 * TODO Sprint add-item-requests.
 */

@Slf4j
@RestController
@RequestMapping(path = "/requests")
public class ItemRequestController {

    private final ItemRequestService itemRequestService;

    @Autowired
    public ItemRequestController(ItemRequestService itemRequestService) {
        this.itemRequestService = itemRequestService;
    }

    @PostMapping
    public ItemRequest create(@RequestHeader("X-Sharer-User-Id") Long requestorId, @RequestBody @Valid ItemRequest itemRequestDto) {
        log.info("Пользователь по ID: {} создает на вещь запрос: {}", requestorId,  itemRequestDto);
        return itemRequestService.create(requestorId, itemRequestDto);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getItemRequestById(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable("requestId") Long requestId) {
        return itemRequestService.getItemRequestDtoById(requestId, userId);
    }

    // вернуть все запросы на вещи пользователя по его ID
    @GetMapping
    public List<ItemRequestDto> getAllRequestByRequestorId(@RequestHeader("X-Sharer-User-Id") Long requestorId) {
        log.info("Возвращаем пользователю по ID: {} его запросы на вещи", requestorId);
        return itemRequestService.getAllRequestByRequestorId(requestorId);
    }

    //вернуть все запросы на вещи кроме его собственных
    @GetMapping("/all")
    public List<ItemRequestDto> getAllItemRequest(@RequestParam(name = "from", defaultValue = "0") int from,
                                       @RequestParam(name = "size", defaultValue = "10") int size,
                                       @RequestHeader("X-Sharer-User-Id") Long requestorId) {
        return itemRequestService.getAllItemRequest(from, size, requestorId);
    }
}