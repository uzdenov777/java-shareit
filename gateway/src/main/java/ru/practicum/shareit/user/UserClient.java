package ru.practicum.shareit.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.BaseClient;
import ru.practicum.shareit.user.model.dto.UserDto;

@Service
public class UserClient extends BaseClient {
    private static final String API_PREFIX = "/users";

    @Autowired
    public UserClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    public ResponseEntity<Object> addUser(UserDto userDto) {
        ResponseEntity<Object> res = post("", userDto);
        return ResponseEntity.status(res.getStatusCode())
                .body(res.getBody());
    }

    public ResponseEntity<Object> updateUser(Long userId, UserDto userDto) {
        String path = "/" + userId;
        ResponseEntity<Object> res = patch(path, userDto);

        return ResponseEntity.status(res.getStatusCode())
                .body(res.getBody());
    }

    public ResponseEntity<Object> removeUser(Long userId) {
        String path = "/" + userId;
        ResponseEntity<Object> res = delete(path);

        return ResponseEntity.status(res.getStatusCode())
                .body(res.getBody());
    }

    public ResponseEntity<Object> getUserById(Long userId) {
        String path = "/" + userId;
        ResponseEntity<Object> res = get(path);

        return ResponseEntity.status(res.getStatusCode())
                .body(res.getBody());
    }

    public ResponseEntity<Object> getAllUsers() {
        ResponseEntity<Object> res = get("");

        return ResponseEntity.status(res.getStatusCode())
                .body(res.getBody());
    }
}