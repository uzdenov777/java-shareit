package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.BaseClient;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.HashMap;
import java.util.Map;

@Service
public class ItemRequestClient extends BaseClient {

    private static final String API_PREFIX = "/requests";

    public ItemRequestClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder.uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    public ResponseEntity<Object> create(Long requestorId, @Valid ItemRequest itemRequestDto) {
        ResponseEntity<Object> response = post("", requestorId, itemRequestDto);

        return response;
    }

    public ResponseEntity<Object> getByRequestor(Long requestorId) {
        ResponseEntity<Object> response = get("", requestorId);

        return response;
    }

    public ResponseEntity<Object> getAll(int from, int size, Long requestorId) {
        String path = "/all?from={from}&size={size}";

        Map<String, Object> params = new HashMap<>();
        params.put("from", from);
        params.put("size", size);

        ResponseEntity<Object> response = get(path, requestorId, params);

        return response;
    }


    public ResponseEntity<Object> getItemRequestDtoById(Long requestId, Long userId) {
        String path = "/" + requestId;

        ResponseEntity<Object> response = get(path, userId);

        return response;
    }
}