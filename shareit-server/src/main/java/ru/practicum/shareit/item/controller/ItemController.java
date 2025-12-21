package ru.practicum.shareit.item.controller;

import jakarta.validation.Valid;
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
        return itemService.add(ownerId, newItemDto);
    }

    @PatchMapping("/{itemId}")
    public ItemResponse updateItem(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable Long itemId, @RequestBody ItemDto itemDto) {
        return itemService.updateItem(userId, itemId, itemDto);
    }

    @GetMapping("/{itemId}")
    public ItemResponse getItemById(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable Long itemId) {
        ItemResponse res = itemService.getItemResponseByIdFromUser(userId ,itemId);
        System.out.println(res);
        return res;
    }

    @GetMapping
    public List<ItemResponse> getAllItemsFromUser(@RequestHeader("X-Sharer-User-Id") Long userId,
                                                  @RequestParam(name = "from", defaultValue = "0") int from,
                                                  @RequestParam(name = "size", defaultValue = "10") int size) {
        return itemService.getAllItemsFromUser(from, size, userId);
    }

    @GetMapping("/search")
    public List<ItemResponse> searchItem(@RequestParam(defaultValue = "") String text,
                                         @RequestHeader("X-Sharer-User-Id") Long userId,
                                         @RequestParam(name = "from", defaultValue = "0") int from,
                                         @RequestParam(name = "size", defaultValue = "10") int size) {
        return itemService.itemSearch(text, userId, from, size);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> addComment(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable Long itemId, @RequestBody CommentRequest commentRequest) {
        String textComment = commentRequest.getText();

        log.info("Добавление комментария: {} для вещи по ID: {} пользователем по ID: {}", textComment, itemId, userId);

        if (textComment.isBlank()) {
            log.error("Передан пустой текст комментария для вещи по ID: {} пользователем по ID: {}", itemId, userId);
            Map<String, String> errors = new HashMap<>();
            errors.put("errors", "Не верно переданные данные в теле");

            return ResponseEntity.badRequest().body(errors);
        }

        CommentResponse comment = itemService.addComment(userId, itemId, textComment);
        return ResponseEntity.ok(comment);
    }
}