package ru.practicum.shareit.request.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.model.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

/**
 * TODO Sprint add-item-requests.
 */

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
        return itemRequestService.create(requestorId, itemRequestDto);
    }

    @GetMapping
    public List<ItemRequestDto> getByRequestorId(@RequestHeader("X-Sharer-User-Id") Long requestorId) {
        return itemRequestService.getByRequestor(requestorId);
    }

    @GetMapping("/all")
    public List<ItemRequestDto> getAll(@RequestParam(name = "from", defaultValue = "0") int from
            ,@RequestParam(name = "size", defaultValue = "10") int size
            ,@RequestHeader("X-Sharer-User-Id") Long requestorId) {
        return itemRequestService.getAll(from, size, requestorId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getById(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable("requestId") Long requestId) {
        return itemRequestService.getItemRequestDtoById(requestId, userId);
    }
}