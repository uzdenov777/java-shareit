package ru.practicum.shareit.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.BaseClient;
import ru.practicum.shareit.item.model.dto.CommentRequest;
import ru.practicum.shareit.item.model.dto.ItemDto;

import java.util.HashMap;
import java.util.Map;

@Service
public class ItemClient extends BaseClient {
    private static final String API_PREFIX = "/items";

    @Autowired
    public ItemClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    public ResponseEntity<Object> add(Long ownerId, ItemDto newItemDto) {

        ResponseEntity<Object> res = post("", ownerId, newItemDto);
        return res;
    }

    public ResponseEntity<Object> updateItem(Long userId, Long itemId, ItemDto itemDto) {
        String path = "/" + itemId;

        ResponseEntity<Object> res = patch(path, userId, itemDto);
        return res;
    }

    public ResponseEntity<Object> getItemResponseByIdFromUser(Long userId, Long itemId) {
        String path = "/" + itemId;

        ResponseEntity<Object> res = get(path, userId);
        return res;
    }

    public ResponseEntity<Object> getAllItemsFromUser(int from, int size, Long userId) {
        String path = "?from={from}&size={size}";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("from", from);
        parameters.put("size", size);

        ResponseEntity<Object> res = get(path, userId, parameters);
        return res;
    }

    public ResponseEntity<Object> itemSearch(String text, Long userId, int from, int size) {
        String path = "/search?text={text}&from={from}&size={size}";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("text", text);
        parameters.put("from", from);
        parameters.put("size", size);

        ResponseEntity<Object> res = get(path, userId, parameters);
        return res;
    }

    public ResponseEntity<Object> addComment(Long userId, Long itemId, CommentRequest commentRequest) {
        String path = "/" + itemId + "/comment";

        ResponseEntity<Object> res = post(path, userId, commentRequest);
        return res;
    }
}