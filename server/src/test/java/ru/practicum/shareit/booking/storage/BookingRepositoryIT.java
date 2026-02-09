package ru.practicum.shareit.booking.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.util.MyPageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryIT {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    private Long bookerId;
    private Long bookingId;

    @BeforeEach
    void setUp() {
        // Создаем пользователей
        User booker = new User();
        booker.setName("Booker");
        booker.setEmail("booker@gmail.com");
        entityManager.persist(booker);

        User owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@gmail.com");
        entityManager.persist(owner);

        // Сохраняем ID для использования в тестах
        bookerId = booker.getId();

        // Создаем предмет
        Item item = new Item();
        item.setName("Item");
        item.setDescription("Description");
        item.setAvailable(true);
        item.setOwner(owner);
        entityManager.persist(item);

        // Создаем бронирование - ВАЖНО: тоже через entityManager.persist()
        Booking booking = new Booking();
        booking.setBooker(booker);
        booking.setItem(item);
        booking.setStart(LocalDateTime.now().minusDays(1)); // Изменил на прошедшее время
        booking.setEnd(LocalDateTime.now().plusDays(1));
        booking.setStatus(BookingStatus.WAITING);

        entityManager.persist(booking); // ← ИЗМЕНИЛ ЭТУ СТРОКУ!
        entityManager.flush();
        entityManager.clear();

        bookingId = booking.getId();
    }

    @Test
    void findAllByBookerIdOrderByIdDesc_shouldReturnBookingsForSpecificBooker() {
        // Act
        MyPageRequest request = new MyPageRequest(0, 10);

        var result = bookingRepository.findAllByBookerIdOrderByIdDesc(request, bookerId);

        // Assert
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(booking -> {
                    assertThat(booking.getId()).isEqualTo(bookingId);
                    assertThat(booking.getBooker().getId()).isEqualTo(bookerId);
                    assertThat(booking.getStatus()).isEqualTo(BookingStatus.WAITING);
                });
    }

    @Test
    void findCurrentByBookerId() {
    }

    @Test
    void findPastByBookerId() {
    }

    @Test
    void findPastByBookerIdAndItemId() {
    }

    @Test
    void findFutureByBookerId() {
    }

    @Test
    void findWaitingByBookerId() {
    }

    @Test
    void findRejectedByBookerId() {
    }

    @Test
    void findAllByOwnerId() {
    }

    @Test
    void findCurrentByOwnerId() {
    }

    @Test
    void findPastByOwnerId() {
    }

    @Test
    void findFutureByOwnerId() {
    }

    @Test
    void findWaitingByOwnerId() {
    }

    @Test
    void findRejectedByOwnerId() {
    }

    @Test
    void findLastBookingByItemId() {
    }

    @Test
    void findNextBookingByItemId() {
    }
}