package ru.practicum.shareit.item.storage;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.dto.ItemDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class InMemoryItemStorage implements ItemStorage {
    private final Map<Long, Item> items = new HashMap<>();
    private static Long newId = 0L;

    @Override
    public Item add(Item item) {

        Long itemId = getNewId();

        item.setId(itemId);

        items.put(itemId, item);

        return item;
    }

    public Item updateNameItem(Long itemId, ItemDto itemDto) {
        Item item = items.get(itemId);

        item.setName(itemDto.getName());

        return item;
    }

    public Item updateDescriptionItem(Long itemId, ItemDto itemDto) {
        Item item = items.get(itemId);

        item.setDescription(itemDto.getDescription());

        return item;
    }

    public Item updateAvailableItem(Long itemId, ItemDto itemDto) {
        Item item = items.get(itemId);

        item.setAvailable(itemDto.isAvailable());

        return item;
    }

    @Override
    public Item getItemById(Long itemId) {
        return items.get(itemId);
    }

    public List<Item> getAllItemsFromUser(Long userId) {
        List<Item> itemsDtoFromUser = new ArrayList<>();

        for (Item item : items.values()) {
            if (item.getIdOwner().equals(userId)) {
                itemsDtoFromUser.add(item);
            }
        }

        return itemsDtoFromUser;
    }

    public boolean existsItem(Long itemId) {
        return items.containsKey(itemId);
    }

    public boolean isUserOwnerOfItem(Long userId, Long itemId) {
        Item item = items.get(itemId);

        return userId.equals(item.getIdOwner());
    }

    public List<Item> itemSearch(String searchText) {
        List<Item> suitableItems = new ArrayList<>();

        for (Item item : items.values()) {
            String itemName = item.getName().toLowerCase();
            String itemDescription = item.getDescription().toLowerCase();
            boolean itemAvailable = item.getAvailable();

            if ((itemName.contains(searchText.toLowerCase()) || itemDescription.contains(searchText.toLowerCase())) && itemAvailable) {
                suitableItems.add(item);
            }
        }

        return suitableItems;
    }

    private Long getNewId() {
        return ++newId;
    }
}