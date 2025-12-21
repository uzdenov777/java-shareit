package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.model.dto.CommentRequest;
import ru.practicum.shareit.item.model.dto.ItemDto;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO Sprint add-controllers.
 */

@Slf4j
@RestController
@RequestMapping("/items")
public class ItemController {
    private final ItemClient itemClient;

    public ItemController(ItemClient itemClient) {
        this.itemClient = itemClient;
    }

    @PostMapping
    public ResponseEntity<Object> add(@RequestHeader("X-Sharer-User-Id") Long ownerId, @RequestBody @Valid ItemDto newItemDto) {
        return itemClient.add(ownerId, newItemDto);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<Object> updateItem(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable Long itemId, @RequestBody ItemDto itemDto) {
        return itemClient.updateItem(userId, itemId, itemDto);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Object> getItemById(@RequestHeader("X-Sharer-User-Id") Long userId, @PathVariable Long itemId) {
        return itemClient.getItemResponseByIdFromUser(userId, itemId);
    }

    @GetMapping
    public ResponseEntity<Object> getAllItemsFromUser(@RequestHeader("X-Sharer-User-Id") Long userId,
                                                      @RequestParam(name = "from", defaultValue = "0") int from,
                                                      @RequestParam(name = "size", defaultValue = "10") int size) {
        return itemClient.getAllItemsFromUser(from, size, userId);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchItem(@RequestParam(defaultValue = "") String text,
                                             @RequestHeader("X-Sharer-User-Id") Long userId,
                                             @RequestParam(name = "from", defaultValue = "0") int from,
                                             @RequestParam(name = "size", defaultValue = "10") int size) {
        return itemClient.itemSearch(text, userId, from, size);
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

        return itemClient.addComment(userId, itemId, commentRequest);
    }
}