package ru.practicum.shareit.booking.storage;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.util.MyPageRequest;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findAllByBookerIdOrderByIdDesc(MyPageRequest pageRequest, Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start <= CURRENT_TIMESTAMP " +
            "AND b.end >= CURRENT_TIMESTAMP " +
            "AND b.booker.id = :id")
    Page<Booking> findCurrentByBookerId(MyPageRequest pageRequest, @Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.end < CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "AND b.booker.id = :id " +
            "ORDER BY b.id DESC")
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
            "AND b.booker.id = :id " +
            "ORDER BY b.id DESC")
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
            "WHERE b.item.owner.id = :id " +
            "ORDER BY b.id DESC")
    Page<Booking> findAllByOwnerId(MyPageRequest pageRequest, @Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start <= CURRENT_TIMESTAMP " +
            "AND b.end >= CURRENT_TIMESTAMP " +
            "AND b.item.owner.id = :id")
    Page<Booking> findCurrentByOwnerId(MyPageRequest pageRequest, @Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.end < CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "AND b.item.owner.id = :id " +
            "ORDER BY b.id DESC")
    Page<Booking> findPastByOwnerId(MyPageRequest pageRequest, @Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start > CURRENT_TIMESTAMP " +
            "AND b.item.owner.id = :id " +
            "ORDER BY b.id DESC")
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

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.start < CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "ORDER BY b.end DESC " +
            "LIMIT 1")
    Optional<Booking> findLastBookingByItemId(@Param("itemId") Long itemId);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.start > CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "ORDER BY b.start ASC " +
            "LIMIT 1")
    Optional<Booking> findNextBookingByItemId(@Param("itemId") Long itemId);
}