package ru.practicum.shareit.booking;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.BaseClient;
import ru.practicum.shareit.booking.model.dto.BookingRequest;

import java.util.HashMap;
import java.util.Map;

@Service
public class BookingClient extends BaseClient {
    private static final String API_PREFIX = "/bookings";

    @Autowired
    public BookingClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    public ResponseEntity<Object> add(Long bookerId, BookingRequest booking) {
        ResponseEntity<Object> res = post("", bookerId, booking);

        return res;
    }

    public ResponseEntity<Object> confirmingOrRejectingBookingRequest(Long userId, Long bookingId, @NotNull Boolean approved) {
        String path = "/" + bookingId + "?approved={approved}";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approved", approved);

        ResponseEntity<Object> res = patch(path, userId, parameters, null);

        return res;
    }

    public ResponseEntity<Object> getBookingById(Long userId, Long bookingId) {
        String path = "/" + bookingId;

        ResponseEntity<Object> res = get(path, userId);

        return res;
    }

    public ResponseEntity<Object> getListAllBookingsForCurrentUser(Long userId, String state, int from, int size) {
        String path = "?state={state}&from={from}&size={size}";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("state", state);
        parameters.put("from", from);
        parameters.put("size", size);

        ResponseEntity<Object> res = get(path, userId, parameters);

        return ResponseEntity.status(res.getStatusCode())
                .body(res.getBody());
    }


    public ResponseEntity<Object> getListAllBookingsForCurrentOwner(Long userId, String state, int from, int size) {
        String path = "/owner?state={state}&from={from}&size={size}";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("state", state);
        parameters.put("from", from);
        parameters.put("size", size);

        ResponseEntity<Object> res = get(path, userId, parameters);

        return ResponseEntity.status(res.getStatusCode())
                .body(res.getBody());
    }
}