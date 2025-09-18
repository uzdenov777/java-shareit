package ru.practicum.shareit.item.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class ItemService {
    private final ItemRepository itemRepository;

    private final UserService userService;

    public ItemService(ItemRepository itemRepository, UserService userService) {
        this.itemRepository = itemRepository;
        this.userService = userService;
    }

    public Item addItem(Long userId, Item item) {
        boolean isExistsUser = userService.existsUser(userId);
        if (!isExistsUser) {
            log.info("Владелец вещи по ID:{} не найден в базе данных при добавлении новой вещи", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещи по ID:" + userId + " не найден в базе данных при добавлении новой вещи");
        }

        item.setOwnerId(userId);

        Item save = itemRepository.save(item);
        return save;
    }

    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) throws ResponseStatusException {
        Optional<Item> itemOpt = itemRepository.findById(itemId);

        if (itemOpt.isEmpty()) {
            log.info("Вещь для обновления не найдена по ID:{}", itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Вещь для обновления не найдена по ID:" + itemId);
        }

        Item existingItem = itemOpt.get();

        checkValidNewVersionItem(userId, existingItem);

        String nameDto = itemDto.getName();
        String descriptionDto = itemDto.getDescription();
        Boolean availableDto = itemDto.isAvailable();

        boolean isNotEmptyName = StringUtils.hasText(nameDto);
        boolean isNotEmptyDescription = StringUtils.hasText(descriptionDto);
        boolean isNotNullAvailable = availableDto != null;

        if (!isNotEmptyName && !isNotEmptyDescription && !isNotNullAvailable) {
            log.info("Все новые поля пустые для обновления вещи по ID: {}", itemId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Все новые поля пустые для обновления вещи по ID: " + itemId);
        }

        if (isNotEmptyName) {
            existingItem.setName(nameDto);
        }

        if (isNotEmptyDescription) {
            existingItem.setDescription(descriptionDto);
        }

        if (isNotNullAvailable) {
            existingItem.setAvailable(availableDto);
        }

        Item updatedItem = itemRepository.save(existingItem);
        ItemDto itemDtoRes = toDto(updatedItem);

        return itemDtoRes;
    }

    public ItemDto getItemById(Long userId, Long itemId) throws ResponseStatusException {
        boolean isExistsUser = userService.existsUser(userId);
        if (!isExistsUser) {
            log.info("Владелец вещи по ID:{} не найден в базе данных при возврате вещи по ID", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещи по ID:" + userId + " не найден в базе данных при вещи по ID");
        }

        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            log.info("Не найдена вещь по ID:{}", itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдена вещь по ID:" + itemId);
        }

        Item existingItem = itemOpt.get();
        ItemDto itemDtoRes = toDto(existingItem);
        return itemDtoRes;
    }

    public List<ItemDto> getAllItemsFromUser(Long userId) {
        boolean isExistsUser = userService.existsUser(userId);
        if (!isExistsUser) {
            log.info("Владелец вещей по ID:{} не найден в базе данных при возврате всех его вещей", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещей по ID:" + userId + " не найден в базе данных при возврате всех его вещей");
        }

        List<ItemDto> itemsDtoFromUser = new ArrayList<>();
        List<Item> itemsFromUser = itemRepository.findAllByOwnerId(userId);

        for (Item item : itemsFromUser) {
            ItemDto itemDto = toDto(item);
            itemsDtoFromUser.add(itemDto);
        }

        return itemsDtoFromUser;
    }

    public List<ItemDto> itemSearch(String text, Long userId) {
        boolean isExistsUser = userService.existsUser(userId);
        if (!isExistsUser) {
            log.info("Пользователь по ID:{} не зарегистрирован для поиска вещей по тексту", userId);
            return new ArrayList<>();
        }

        if (text.isBlank()) {
            return new ArrayList<>();
        }

        List<ItemDto> suitableItemsDto = new ArrayList<>();
        List<Item> suitableItems = itemRepository.findAvailableItemsBySearchText(text);

        for (Item item : suitableItems) {
            suitableItemsDto.add(toDto(item));
        }

        return suitableItemsDto;
    }

    private void checkValidNewVersionItem(Long userId, Item item) throws ResponseStatusException {
        if (Objects.isNull(userId)) {
            log.info("При обновлении вещи не может отсутствовать ID владельца");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "При обновлении пользователя не может отсутствовать ID владельца");
        }

        Long itemId = item.getId();
        Long ownerId = item.getOwnerId();
        boolean isUserOwner = userId.equals(ownerId);
        boolean isExistsUser = userService.existsUser(userId);

        if (!isExistsUser) {
            log.info("Владелец вещи по ID:{} не найден в базе данных при обновлении вещи по ID:{}", userId, itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещи по ID:" + userId + " не найден в базе данных при обновлении вещи по ID:" + itemId);
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