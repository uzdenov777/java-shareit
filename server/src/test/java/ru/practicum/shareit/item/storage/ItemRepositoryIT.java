package ru.practicum.shareit.item.storage;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.util.MyPageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ItemRepositoryIT {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ItemRepository itemRepository;

    private Item item;

    private User owner;

    private Long ownerId;
    private MyPageRequest pageRequest;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@email");
        entityManager.persist(owner);
        ownerId = owner.getId();

        item = new Item();
        item.setName("Item");
        item.setDescription("Description");
        item.setAvailable(true);
        item.setOwner(owner);
        entityManager.persist(item);

        pageRequest = new MyPageRequest(0, 10);
    }

    @AfterEach
    void cleanup() {
        entityManager.clear();
    }

    @Test
    void findAllByOwnerIdOrderByIdAsc_whenExistItem_thenReturnNotEmptyList() {
        //when
        Page<Item> response = itemRepository.findAllByOwnerIdOrderByIdAsc(pageRequest, ownerId);

        //then
        List<Item> items = response.getContent();
        Item resultItem = items.get(0);

        assertEquals(item, resultItem);
        assertEquals(1, items.size());
    }

    @Test
    void findAllByOwnerIdOrderByIdAsc_whenNonExistItem_thenReturnEmptyList() {
        //given
        //у пользователя нет больше вещей
        entityManager.remove(item);

        //when
        Page<Item> response = itemRepository.findAllByOwnerIdOrderByIdAsc(pageRequest, ownerId);

        //then
        List<Item> items = response.getContent();

        assertTrue(items.isEmpty());
    }

    @Test
    void findAvailableItemsBySearchText_whenItemNameFits_thenReturnNotEmptyList() {
        //given
        //сделали так, чтобы найти объект по имени и он доступен
        String searchText = item.getName();

        //when
        Page<Item> response = itemRepository.findAvailableItemsBySearchText(pageRequest, searchText);

        //then
        List<Item> items = response.getContent();
        Item resultItem = items.get(0);

        assertEquals(item, resultItem);
        assertEquals(1, items.size());
    }

    @Test
    void findAvailableItemsBySearchText_whenItemDescriptionFits_thenReturnNotEmptyList() {
        //given
        //сделали так, чтобы найти объект по имени и он доступен
        String searchText = item.getName();

        //when
        Page<Item> response = itemRepository.findAvailableItemsBySearchText(pageRequest, searchText);

        //then
        List<Item> items = response.getContent();
        Item resultItem = items.get(0);

        assertEquals(item, resultItem);
        assertEquals(1, items.size());
    }

    @Test
    void findAvailableItemsBySearchText_whenItemDescriptionAndNameNonFits_thenReturnEmptyList() {
        //given
        //имя и описание не подходит, но он доступен
        String searchText = "дрель";

        //when
        Page<Item> response = itemRepository.findAvailableItemsBySearchText(pageRequest, searchText);

        //then
        List<Item> items = response.getContent();

        assertTrue(items.isEmpty());
    }

    @Test
    void findAvailableItemsBySearchText_whenItemNonAvailable_thenReturnEmptyList() {
        //given
        //имя или описание подходит, но он не доступен
        String searchText = item.getName();
        item.setAvailable(false);

        //when
        Page<Item> response = itemRepository.findAvailableItemsBySearchText(pageRequest, searchText);

        //then
        List<Item> items = response.getContent();

        assertTrue(items.isEmpty());
    }
}