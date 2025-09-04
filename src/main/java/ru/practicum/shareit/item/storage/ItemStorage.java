package ru.practicum.shareit.item.storage;

import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.dto.ItemDto;

public interface ItemStorage {

    Item add(Item item);

    Item getItemById(Long itemId);

    Item delete(Long itemId);
}