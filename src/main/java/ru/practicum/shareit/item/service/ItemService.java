package ru.practicum.shareit.item.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import ru.practicum.shareit.item.model.dto.ItemResponse;
import ru.practicum.shareit.item.storage.CommentRepository;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;
import ru.practicum.shareit.util.MyPageRequest;

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
    private final ItemRequestService itemRequestService;
    private final UserService userService;

    @Autowired
    public ItemService(ItemRepository itemRepository, CommentRepository commentRepository, BookingRepository bookingRepository, ItemRequestService itemRequestService, UserService userService) {
        this.itemRepository = itemRepository;
        this.commentRepository = commentRepository;
        this.bookingRepository = bookingRepository;
        this.itemRequestService = itemRequestService;
        this.userService = userService;
    }

    public ItemResponse add(Long ownerId, ItemDto itemDto) {
        Item newItem = toItem(ownerId, itemDto);

        Item save = itemRepository.save(newItem);

        ItemResponse response = toItemResponse(save);

        return response;
    }

    public ItemResponse updateItem(Long userId, Long itemId, ItemDto itemDto) throws ResponseStatusException {
        Optional<Item> itemOpt = itemRepository.findById(itemId);

        if (itemOpt.isEmpty()) {
            log.info("Вещь для обновления не найдена по ID:{}", itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Вещь для обновления не найдена по ID:" + itemId);
        }

        Item existingItem = itemOpt.get();

        checkValidNewVersionItem(userId, existingItem);

        String nameDto = itemDto.getName();
        String descriptionDto = itemDto.getDescription();
        Boolean availableDto = itemDto.getAvailable();

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
        ItemResponse response = toItemResponse(updatedItem);

        return response;
    }

    public ItemResponse getItemResponseById(Long itemId) throws ResponseStatusException {

        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            log.info("Не найдена вещь по ID:{}", itemId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдена вещь по ID:" + itemId);
        }

        Item existingItem = itemOpt.get();
        ItemResponse response = toItemResponse(existingItem);

        return response;
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

    public List<ItemResponse> getAllItemsFromUser(int from, int size, Long userId) {
        boolean isExistsUser = userService.existsUser(userId);
        if (!isExistsUser) {
            log.info("Владелец вещей по ID:{} не найден в базе данных при возврате всех его вещей", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Владелец вещей по ID:" + userId + " не найден в базе данных при возврате всех его вещей");
        }

        MyPageRequest pageRequest = new MyPageRequest(from, size);
        Page<Item> page = itemRepository.findAllByOwnerId(pageRequest, userId);
        List<Item> itemsFromUser = page.getContent();

        List<ItemResponse> itemsDtoFromUser = new ArrayList<>();
        for (Item item : itemsFromUser) {
            ItemResponse itemResponse = toItemResponse(item);
            itemsDtoFromUser.add(itemResponse);
        }

        return itemsDtoFromUser;
    }

    public List<ItemResponse> itemSearch(String text, Long userId, int from, int size) {
        boolean isExistsUser = userService.existsUser(userId);
        if (!isExistsUser) {
            log.info("Пользователь по ID:{} не зарегистрирован для поиска вещей по тексту", userId);
            return new ArrayList<>();
        }

        if (text.isBlank()) {
            return new ArrayList<>();
        }

        MyPageRequest pageRequest = new MyPageRequest(from, size);
        Page<Item> page = itemRepository.findAvailableItemsBySearchText(pageRequest, text);
        List<Item> suitableItems = page.getContent();

        List<ItemResponse> suitableItemsDto = new ArrayList<>();
        for (Item item : suitableItems) {
            suitableItemsDto.add(toItemResponse(item));
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

    private Item toItem(Long ownerId, ItemDto itemDto) throws ResponseStatusException {
        User owner = userService.getUserById(ownerId);
        String name = itemDto.getName();
        String description = itemDto.getDescription();
        Boolean available = itemDto.getAvailable();
        ItemRequest itemRequest;

        Long itemRequestId = itemDto.getRequestId();
        if (itemRequestId != null) {
            itemRequest = itemRequestService.getItemRequestById(itemRequestId);
        } else {
            itemRequest = null;
        }

        Item newItem = new Item();

        newItem.setRequest(itemRequest);
        newItem.setOwner(owner);
        newItem.setName(name);
        newItem.setDescription(description);
        newItem.setAvailable(available);

        return newItem;
    }

    private ItemResponse toItemResponse(Item item) {
        Long id = item.getId();
        String name = item.getName();
        String description = item.getDescription();
        boolean available = item.getAvailable();
        List<Comment> comments = item.getComments();
        ItemRequest request = item.getRequest();
        Long requestId;

        if (Objects.nonNull(request)) {
            requestId = request.getId();
        } else {
            requestId = null;
        }

        List<CommentResponse> commentResponseList = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponse commentResponse = toCommentResponse(comment);

            commentResponseList.add(commentResponse);
        }

        ItemResponse response = new ItemResponse();
        response.setId(id);
        response.setName(name);
        response.setDescription(description);
        response.setAvailable(available);
        response.setRequestId(requestId);
        response.setComments(commentResponseList);

        return response;
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