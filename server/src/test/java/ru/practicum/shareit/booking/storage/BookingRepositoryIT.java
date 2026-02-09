package ru.practicum.shareit.booking.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.util.MyPageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BookingRepositoryIT {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    private User booker;
    private User owner;
    private Item item;
    private Booking booking;

    private MyPageRequest myPageRequest;
    private Long bookerId;
    private Long ownerId;
    private Long itemId;
    private Long bookingId;

    @BeforeEach
    void setUp() {
        booker = new User();
        booker.setName("Booker");
        booker.setEmail("booker@gmail.com");
        entityManager.persist(booker);
        bookerId = booker.getId();

        owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@gmail.com");
        entityManager.persist(owner);
        ownerId = owner.getId();

        item = new Item();
        item.setName("Item");
        item.setDescription("Description");
        item.setAvailable(true);
        item.setOwner(owner);
        entityManager.persist(item);
        itemId = item.getId();

        booking = new Booking();
        booking.setBooker(booker);
        booking.setItem(item);
        booking.setStart(LocalDateTime.now().minusDays(1));
        booking.setEnd(LocalDateTime.now().plusDays(1));
        booking.setStatus(BookingStatus.WAITING);
        entityManager.persist(booking);
        bookingId = booking.getId();

        myPageRequest = new MyPageRequest(0, 10);
    }

    @AfterEach
    void cleanUp() {
        entityManager.clear();
    }

    @Test
    void findAllByBookerIdOrderByIdDesc_whenBookerHasBookings_thenReturnNotEmptyListBookings() {
        //when
        Page<Booking> response = bookingRepository.findAllByBookerIdOrderByIdDesc(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        Booking resultBooking = bookings.get(0);

        assertEquals(booking, resultBooking);
        assertEquals(1, bookings.size());
    }

    @Test
    void findAllByBookerIdOrderByIdDesc_whenBookerNotHasBookings_thenReturnEmptyListBookings() {
        //given
        entityManager.remove(booking);//удалим бронирование, чтобы получить пустой список

        //when
        Page<Booking> response = bookingRepository.findAllByBookerIdOrderByIdDesc(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();

        assertTrue(bookings.isEmpty());
    }

    @Test
    void findCurrentByBookerId_whenCurrentBookingExists_thenReturnBooking() {
        //given
        //установили время бронирования на настоящее
        booking.setStart(LocalDateTime.now());
        booking.setEnd(LocalDateTime.now().plusDays(1));

        //when
        Page<Booking> response = bookingRepository.findCurrentByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        Booking resultBooking = bookings.get(0);

        assertEquals(booking, resultBooking);
        assertEquals(1, bookings.size());
    }

    @Test
    void findCurrentByBookerId_whenCurrentBookingNotExists_thenReturnEmptyListBookings() {
        //given
        //сделали так, чтобы не было текущих бронирований
        booking.setStart(LocalDateTime.now().plusHours(1));
        booking.setEnd(LocalDateTime.now().plusDays(1));

        //when
        Page<Booking> response = bookingRepository.findCurrentByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        assertTrue(bookings.isEmpty());
    }

    @Test
    void findPastByBookerId_whenPastBookingExists_thenReturnBooking() {
        //given
        //бронирование завершено в прошлом и APPROVED
        booking.setStart(LocalDateTime.now().minusDays(1));
        booking.setEnd(LocalDateTime.now().minusHours(1));
        booking.setStatus(BookingStatus.APPROVED);

        //when
        Page<Booking> response = bookingRepository.findPastByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        Booking resultBooking = bookings.get(0);

        assertEquals(booking, resultBooking);
        assertEquals(1, bookings.size());
    }

    @Test
    void findPastByBookerId_whenBookingNotPast_thenReturnEmptyListBookings() {
        //given
        //бронирование не в прошлом, но APPROVED
        booking.setStart(LocalDateTime.now().plusHours(1));
        booking.setEnd(LocalDateTime.now().plusDays(1));
        booking.setStatus(BookingStatus.APPROVED);

        //when
        Page<Booking> response = bookingRepository.findPastByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        assertTrue(bookings.isEmpty());
    }

    @Test
    void findPastByBookerId_whenBookingStatusNotApproved_thenReturnEmptyListBookings() {
        //given
        //бронирование в прошлом, но не APPROVED
        booking.setStart(LocalDateTime.now().minusDays(1));
        booking.setEnd(LocalDateTime.now().minusHours(1));
        booking.setStatus(BookingStatus.REJECTED);

        //when
        Page<Booking> response = bookingRepository.findPastByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        assertTrue(bookings.isEmpty());
    }

    @Test
    void findPastByBookerIdAndItemId_whenBookingExist_thenReturnTrue() {
        //given
        //когда booker пользовался вещью
        booking.setStart(LocalDateTime.now().minusDays(1));
        booking.setEnd(LocalDateTime.now().minusHours(1));
        booking.setStatus(BookingStatus.APPROVED);

        //when
        Boolean response = bookingRepository.findPastByBookerIdAndItemId(bookerId, itemId);

        //then
        assertTrue(response);
    }

    @Test
    void findPastByBookerIdAndItemId_whenBookingIsNotInThePast_thenReturnFalse() {
        //given
        //когда бронирование не в прошлом
        booking.setStart(LocalDateTime.now().plusHours(1));
        booking.setEnd(LocalDateTime.now().plusDays(1));
        booking.setStatus(BookingStatus.APPROVED);

        //when
        Boolean response = bookingRepository.findPastByBookerIdAndItemId(bookerId, itemId);

        //then
        assertFalse(response);
    }

    @Test
    void findPastByBookerIdAndItemId_whenBookingRejected_thenReturnFalse() {
        //given
        //когда бронирование отклонено, но в прошлом и тп
        booking.setStart(LocalDateTime.now().plusHours(1));
        booking.setEnd(LocalDateTime.now().plusDays(1));
        booking.setStatus(BookingStatus.REJECTED);

        //when
        Boolean response = bookingRepository.findPastByBookerIdAndItemId(bookerId, itemId);

        //then
        assertFalse(response);
    }

    @Test
    void findPastByBookerIdAndItemId_whenUserNotRentalItem_thenReturnFalse() {
        //given
        //когда бронирование было, но у этого пользователя
        booking.setStart(LocalDateTime.now().minusDays(1));
        booking.setEnd(LocalDateTime.now().minusHours(1));
        booking.setStatus(BookingStatus.APPROVED);

        //when
        //специально указал пользователя, который не брал в аренду этот предмет
        Boolean response = bookingRepository.findPastByBookerIdAndItemId(ownerId, itemId);

        //then
        assertFalse(response);
    }

    @Test
    void findFutureByBookerId_whenExistBookingInFuture_thenReturnNotEmptyList() {
        //given
        //бронирование теперь в будущем
        booking.setStart(LocalDateTime.now().plusHours(1));
        booking.setEnd(LocalDateTime.now().plusDays(1));

        //when
        Page<Booking> response = bookingRepository.findFutureByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        Booking resultBooking = bookings.get(0);

        assertEquals(booking, resultBooking);
        assertEquals(1, bookings.size());
    }

    @Test
    void findFutureByBookerId_whenNotExistBookingInFuture_thenReturnEmptyList() {
        //given
        //будущих бронирований нет больше
        booking.setStart(LocalDateTime.now().minusDays(1));
        booking.setEnd(LocalDateTime.now().minusHours(1));

        //when
        Page<Booking> response = bookingRepository.findFutureByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        assertTrue(bookings.isEmpty());
    }

    @Test
    void findWaitingByBookerId_whenWaitingBookingsExist_thenReturnNotEmptyList() {
        //when
        Page<Booking> response = bookingRepository.findWaitingByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        Booking resultBooking = bookings.get(0);

        assertEquals(booking, resultBooking);
        assertEquals(1, bookings.size());
    }

    @Test
    void findWaitingByBookerId_whenWaitingBookingsNotExist_thenReturnNotEmptyList() {
        //given
        //когда нет ожидающих ответа бронирований
        booking.setStatus(BookingStatus.REJECTED);

        //when
        Page<Booking> response = bookingRepository.findWaitingByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        assertTrue(bookings.isEmpty());
    }

    @Test
    void findRejectedByBookerId_whenRejectedBookingsExist_thenReturnNotEmptyList() {
        //given
        //когда есть отклоненные бронирования
        booking.setStatus(BookingStatus.REJECTED);

        //when
        Page<Booking> response = bookingRepository.findRejectedByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        Booking resultBooking = bookings.get(0);

        assertEquals(booking, resultBooking);
        assertEquals(1, bookings.size());
    }

    @Test
    void findRejectedByBookerId_whenRejectedBookingsNotExist_thenReturnNotEmptyList() {
        //given
        //когда нет отклоненных бронирований
        booking.setStatus(BookingStatus.APPROVED);

        //when
        Page<Booking> response = bookingRepository.findRejectedByBookerId(myPageRequest, bookerId);

        //then
        List<Booking> bookings = response.getContent();
        assertTrue(bookings.isEmpty());
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