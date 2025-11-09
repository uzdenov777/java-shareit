package ru.practicum.shareit.booking.storage;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.util.MyPageRequest;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findAllByBookerId(MyPageRequest pageRequest, Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start <= CURRENT_TIMESTAMP " +
            "AND b.end >= CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED'" +
            "AND b.booker.id = :id")
    Page<Booking> findCurrentByBookerId(MyPageRequest pageRequest, @Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.end < CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "AND b.booker.id = :id")
    Page<Booking> findPastByBookerId(MyPageRequest pageRequest, @Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.end < CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "AND b.booker.id = :id " +
            "AND b.item.id = :itemId")
    List<Booking> findPastByBookerIdAndItemId(@Param("id") Long bookerId, @Param("itemId") Long itemId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start > CURRENT_TIMESTAMP " +
            "AND b.booker.id = :id")
    Page<Booking> findFutureByBookerId(MyPageRequest pageRequest, @Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.status = 'WAITING'" +
            "AND b.booker.id = :id")
    Page<Booking> findWaitingByBookerId(MyPageRequest pageRequest, @Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.status = 'REJECTED'" +
            "AND b.booker.id = :id")
    Page<Booking> findRejectedByBookerId(MyPageRequest pageRequest, @Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.item.owner.id = :id")
    Page<Booking> findAllByOwnerId(MyPageRequest pageRequest, @Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start <= CURRENT_TIMESTAMP " +
            "AND b.end >= CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED'" +
            "AND b.item.owner.id = :id")
    Page<Booking> findCurrentByOwnerId(MyPageRequest pageRequest, @Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.end < CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "AND b.item.owner.id = :id")
    Page<Booking> findPastByOwnerId(MyPageRequest pageRequest, @Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start > CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "AND b.item.owner.id = :id")
    Page<Booking> findFutureByOwnerId(MyPageRequest pageRequest, @Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.status = 'WAITING'" +
            "AND b.item.owner.id = :id")
    Page<Booking> findWaitingByOwnerId(MyPageRequest pageRequest, @Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.status = 'REJECTED'" +
            "AND b.item.owner.id = :id")
    Page<Booking> findRejectedByOwnerId(MyPageRequest pageRequest, @Param("id") Long ownerId);
}