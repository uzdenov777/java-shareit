package ru.practicum.shareit.item.storage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.item.model.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findAllByOwnerId(PageRequest pageRequest, Long userId);

    @Query("SELECT i " +
            "FROM Item i " +
            "WHERE i.available = true AND " +
            "(LOWER(i.name) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :text, '%')))")
    Page<Item> findAvailableItemsBySearchText(PageRequest pageRequest, @Param("text") String text);
}