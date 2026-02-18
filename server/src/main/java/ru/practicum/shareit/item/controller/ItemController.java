package ru.practicum.shareit.item.controller;

import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.model.dto.CommentRequest;
import ru.practicum.shareit.item.model.dto.CommentResponse;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.item.model.dto.ItemResponse;
import ru.practicum.shareit.item.service.ItemService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO Sprint add-controllers.
 */

@Slf4j
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ItemResponse add(@RequestHeader("X-Sharer-User-Id") Long ownerId, @RequestBody @Valid ItemDto newItemDto) {
        log.info("Добавление вещи: {} пользователем по ID: {}", newItemDto, ownerId);

        return itemService.add(ownerId, newItemDto);
    }

    @PatchMapping("/{itemId}")
    public ItemResponse updateItem(@RequestHeader("X-Sharer-User-Id") Long ownerId, @PathVariable Long itemId, @RequestBody @NonNull ItemDto itemDto) {
        log.info("Обновление вещи по ID: {} пользователем по ID: {}", itemId, ownerId);

        return itemService.updateItem(ownerId, itemId, itemDto);
    }

    @GetMapping("/{itemId}")
    public ItemResponse getItemById(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable Long itemId) {
        log.info("Возращение вещи по ID: {}", itemId);

        ItemResponse res = itemService.getItemResponseByIdFromUser(userId, itemId);
        return res;
    }

    @GetMapping
    public List<ItemResponse> getAllItemsFromUser(@RequestHeader("X-Sharer-User-Id") Long userId,
                                                  @RequestParam(name = "from", defaultValue = "0") int from,
                                                  @RequestParam(name = "size", defaultValue = "10") int size) {
        return itemService.getAllItemsFromUser(userId, from, size);
    }

    @GetMapping("/search")
    public List<ItemResponse> searchItem(@RequestParam(name = "text", defaultValue = "") String text,
                                         @RequestHeader("X-Sharer-User-Id") Long userId,
                                         @RequestParam(name = "from", defaultValue = "0") int from,
                                         @RequestParam(name = "size", defaultValue = "10") int size) {
        return itemService.itemSearch(text, userId, from, size);
    }

    @PostMapping("/{itemId}/comment")
    public CommentResponse addComment(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable Long itemId, @RequestBody @Valid @NonNull CommentRequest commentRequest) {
        String textComment = commentRequest.getText();

        log.info("Добавление комментария: {} для вещи по ID: {} пользователем по ID: {}", textComment, itemId, userId);

        CommentResponse comment = itemService.addComment(userId, itemId, textComment);
        return comment;
    }
}