package ru.practicum.shareit.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.item.storage.InMemoryItemStorage;
import ru.practicum.shareit.user.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ItemService {
    private final InMemoryItemStorage inMemoryItemStorage;

    private final UserService userService;

    public ItemService(InMemoryItemStorage inMemoryItemStorage, UserService userService) {
        this.inMemoryItemStorage = inMemoryItemStorage;
        this.userService = userService;
    }

    public Item addItem(Long userId, Item item) {
        boolean isExistsUser = userService.isExistsUser(userId);
        if (!isExistsUser) {
            log.info("Владелец вещи по ID:{} не найден в базе данных при добавлении новой вещи", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещи по ID:" + userId + " не найден в базе данных при добавлении новой вещи");
        }

        item.setIdOwner(userId);

        return inMemoryItemStorage.add(item);
    }

    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) throws ResponseStatusException {
        checkValidNewVersionItem(userId, itemId, itemDto); //

        if (Objects.isNull(itemDto.getName())) {
            itemDto.setName("");
        }

        if (Objects.isNull(itemDto.getDescription())) {
            itemDto.setDescription("");
        }

        String name = itemDto.getName();
        String description = itemDto.getDescription();
        Boolean available = itemDto.isAvailable();

        boolean isBlankName = name.isBlank();
        boolean isBlankDescription = description.isBlank();
        boolean isNullAvailable = Objects.isNull(available);

        Item itemToUpdate = null;

        if (!isBlankName) {
            itemToUpdate = inMemoryItemStorage.updateNameItem(itemId, itemDto);
        }

        if (!isBlankDescription) {
            itemToUpdate = inMemoryItemStorage.updateDescriptionItem(itemId, itemDto);
        }

        if (!isNullAvailable) {
            itemToUpdate = inMemoryItemStorage.updateAvailableItem(itemId, itemDto);
        }

        if (isBlankName && isBlankDescription && isNullAvailable) {
            log.info("Все новые поля пустые для обновления вещи по ID: {}", itemId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Все новые поля пустые для обновления вещи по ID: " + itemId);
        }

        return toDto(itemToUpdate);
    }

    public ItemDto getItemById(Long userId, Long itemId) throws ResponseStatusException {
        boolean isExistsUser = userService.isExistsUser(userId);
        boolean isExistsItem = inMemoryItemStorage.existsItem(itemId);
        if (!isExistsUser) {
            log.info("Владелец вещи по ID:{} не найден в базе данных при возврате вещи по ID", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещи по ID:" + userId + " не найден в базе данных при вещи по ID");
        }

        if (!isExistsItem) {
            log.info("Не найдена вещь по ID:{}", itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдена вещь по ID:" + itemId);
        }

        return toDto(inMemoryItemStorage.getItemById(itemId));
    }

    public List<ItemDto> getAllItemsFromUser(Long userId) {
        boolean isExistsUser = userService.isExistsUser(userId);
        if (!isExistsUser) {
            log.info("Владелец вещей по ID:{} не найден в базе данных при возврате всех его вещей", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещей по ID:" + userId + " не найден в базе данных при возврате всех его вещей");
        }

        List<ItemDto> itemsDtoFromUser = new ArrayList<>();
        List<Item> itemsFromUser = inMemoryItemStorage.getAllItemsFromUser(userId);

        for (Item item : itemsFromUser) {

            itemsDtoFromUser.add(
                    toDto(item));
        }

        return itemsDtoFromUser;
    }

    public List<ItemDto> itemSearch(String text, Long userId) {
        boolean isExistsUser = userService.isExistsUser(userId);
        if (!isExistsUser) {
            log.info("Пользователь по ID:{} не зарегистрирован для поиска вещей по тексту", userId);
            return new ArrayList<>();
        }

        if (text.isBlank()) {
            return new ArrayList<>();
        }

        List<ItemDto> suitableItemsDto = new ArrayList<>();
        List<Item> suitableItems = inMemoryItemStorage.itemSearch(text);

        for (Item item : suitableItems) {
            suitableItemsDto.add(toDto(item));
        }

        return suitableItemsDto;
    }

    private void checkValidNewVersionItem(Long userId, Long itemId, ItemDto itemDto) throws ResponseStatusException {
        if (Objects.isNull(userId)) {
            log.info("При обновлении вещи не может отсутствовать ID владельца");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "При обновлении пользователя не может отсутствовать ID владельца");
        }

        boolean isExistsUser = userService.isExistsUser(userId);
        boolean isExistsItem = inMemoryItemStorage.existsItem(itemId);
        boolean isUserOwner = inMemoryItemStorage.isUserOwnerOfItem(userId, itemId);

        if (!isExistsUser) {
            log.info("Владелец вещи по ID:{} не найден в базе данных при обновлении вещи по ID:{}", userId, itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещи по ID:" + userId + " не найден в базе данных при обновлении вещи по ID:" + itemId);
        }

        if (!isExistsItem) {
            log.info("Вещь для обновления не найдена по ID:{}", itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Вещь для обновления не найдена по ID:" + itemId);
        }

        if (!isUserOwner) {
            log.info("Пользователь по ID:{} не владелец вещи по ID{}", userId, itemId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь по ID:" + userId + " не владелец вещи по ID:" + itemId);
        }
    }

    private ItemDto toDto(Item item) {
        ItemDto itemDto = new ItemDto();
        itemDto.setId(item.getId());
        itemDto.setName(item.getName());
        itemDto.setDescription(item.getDescription());
        itemDto.setAvailable(item.getAvailable());
        return itemDto;
    }
}