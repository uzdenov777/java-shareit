package ru.practicum.shareit.booking.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.model.Booking;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByBookerId(Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start <= CURRENT_TIMESTAMP " +
            "AND b.end >= CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED'" +
            "AND b.booker.id = :id")
    List<Booking> findCurrentByBookerId(@Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.end < CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "AND b.booker.id = :id")
    List<Booking> findPastByBookerId(@Param("id") Long bookerId);

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
            "AND b.status = 'APPROVED' " +
            "AND b.booker.id = :id")
    List<Booking> findFutureByBookerId(@Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.status = 'WAITING'" +
            "AND b.booker.id = :id")
    List<Booking> findWaitingByBookerId(@Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.status = 'REJECTED'" +
            "AND b.booker.id = :id")
    List<Booking> findRejectedByBookerId(@Param("id") Long bookerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.item.owner.id = :id")
    List<Booking> findAllByOwnerId(@Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start <= CURRENT_TIMESTAMP " +
            "AND b.end >= CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED'" +
            "AND b.item.owner.id = :id")
    List<Booking> findCurrentByOwnerId(@Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.end < CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "AND b.item.owner.id = :id")
    List<Booking> findPastByOwnerId(@Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.start > CURRENT_TIMESTAMP " +
            "AND b.status = 'APPROVED' " +
            "AND b.item.owner.id = :id")
    List<Booking> findFutureByOwnerId(@Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.status = 'WAITING'" +
            "AND b.item.owner.id = :id")
    List<Booking> findWaitingByOwnerId(@Param("id") Long ownerId);

    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.status = 'REJECTED'" +
            "AND b.item.owner.id = :id")
    List<Booking> findRejectedByOwnerId(@Param("id") Long ownerId);
}