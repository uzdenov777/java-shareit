package ru.practicum.shareit.item.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.dto.CommentRequest;
import ru.practicum.shareit.item.model.dto.CommentResponse;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.item.storage.CommentRepository;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final CommentRepository commentRepository;
    private final BookingRepository bookingRepository;

    private final UserService userService;

    public ItemService(ItemRepository itemRepository, CommentRepository commentRepository, BookingRepository bookingRepository, UserService userService) {
        this.itemRepository = itemRepository;
        this.commentRepository = commentRepository;
        this.bookingRepository = bookingRepository;
        this.userService = userService;
    }

    public ItemDto add(Long userId, Item item) {
        User user = userService.getUserById(userId);

        item.setOwner(user);

        Item save = itemRepository.save(item);
        ItemDto dto = toDto(save);
        return dto;
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

    public ItemDto getItemDtoById(Long itemId) throws ResponseStatusException {

        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            log.info("Не найдена вещь по ID:{}", itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдена вещь по ID:" + itemId);
        }

        Item existingItem = itemOpt.get();
        ItemDto itemDtoRes = toDto(existingItem);
        return itemDtoRes;
    }

    public Item getItemById(Long itemId) throws ResponseStatusException {
        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            log.info("Не найдена вещь по ID:{}", itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдена вещь по ID:" + itemId);
        }

        Item existingItem = itemOpt.get();
        return existingItem;
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

    public CommentResponse addComment(Long authorId, Long itemId, CommentRequest commentRequest) throws ResponseStatusException {

        boolean checkUserRental = checkUserRentalHistory(authorId, itemId);
        if (!checkUserRental) {
            log.info("Пользователь по ID: {} не имеет право добавить комментарий вещи по ID: {}, так как не брал и не завершил аренду этого предмета", authorId, itemId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Пользователь по ID: " + authorId + " не имеет право добавить комментарий вещи по ID: " + itemId + ", так как не брал и не завершил аренду этого предмета");
        }

        Comment comment = toComment(authorId, itemId, commentRequest);
        comment.setCreated(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        CommentResponse commentResponse = toCommentResponse(savedComment);

        return commentResponse;
    }

    private boolean checkUserRentalHistory(Long authorId, Long itemId) {
        List<Booking> bookings = bookingRepository.findPastByBookerIdAndItemId(authorId, itemId);

        if (bookings.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    private void checkValidNewVersionItem(Long userId, Item item) throws ResponseStatusException {
        if (Objects.isNull(userId)) {
            log.info("При обновлении вещи не может отсутствовать ID владельца");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "При обновлении пользователя не может отсутствовать ID владельца");
        }

        Long itemId = item.getId();
        User owner = item.getOwner();
        Long ownerId = owner.getId();
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
        Long id = item.getId();
        String name = item.getName();
        String description = item.getDescription();
        boolean available = item.getAvailable();
        List<Comment> comments = item.getComments();

        ItemDto itemDto = new ItemDto();
        itemDto.setId(id);
        itemDto.setName(name);
        itemDto.setDescription(description);
        itemDto.setAvailable(available);

        List<CommentResponse> commentResponseList = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponse commentResponse = toCommentResponse(comment);

            commentResponseList.add(commentResponse);
        }

        itemDto.setComments(commentResponseList);

        return itemDto;
    }

    private Comment toComment(Long authorId, Long itemId, CommentRequest commentRequest) {
        Item item = getItemById(itemId);
        User author = userService.getUserById(authorId);
        String textComment = commentRequest.getText();

        Comment comment = new Comment();
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setText(textComment);

        return comment;
    }

    private CommentResponse toCommentResponse(Comment comment) {
        User author = comment.getAuthor();

        Long id = comment.getId();
        String text = comment.getText();
        String authorName = author.getName();
        LocalDateTime createdDate = comment.getCreated();

        CommentResponse commentResponse = new CommentResponse();
        commentResponse.setId(id);
        commentResponse.setAuthorName(authorName);
        commentResponse.setText(text);
        commentResponse.setCreated(createdDate);

        return commentResponse;
    }
}