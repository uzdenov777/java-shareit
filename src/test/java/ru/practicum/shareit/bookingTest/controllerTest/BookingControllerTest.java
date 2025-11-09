package ru.practicum.shareit.bookingTest.controllerTest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.practicum.shareit.booking.controller.BookingController;
import ru.practicum.shareit.booking.service.BookingService;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc
public class BookingControllerTest {
    @MockBean
    private BookingService bookingService;

    @Test
    public void test() {

    }
}