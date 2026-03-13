package ru.practicum.shareit.item.service;

import lombok.AllArgsConstructor;
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
import ru.practicum.shareit.item.model.dto.CommentResponse;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.item.model.dto.ItemResponse;
import ru.practicum.shareit.item.model.dto.ItemResponse.BookingRes;
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
@AllArgsConstructor
@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CommentRepository commentRepository;
    private final BookingRepository bookingRepository;

    private final ItemRequestService itemRequestService;
    private final UserService userService;

    public ItemResponse add(Long ownerId, ItemDto itemDto) {
        log.info("Происходит сохранение вещи пользователем по ID: {}, вещь: {}", ownerId, itemDto);

        boolean isExistsUser = userService.existsUser(ownerId);
        if (!isExistsUser) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Пользователь по ID: " + ownerId + " не найден для сохранения вещи: " + itemDto);
        }

        Item newItem = toItem(ownerId, itemDto);

        Item save = itemRepository.save(newItem);
        return toItemResponse(save);
    }

    public ItemResponse updateItem(Long ownerId, Long itemId, ItemDto itemDto) throws ResponseStatusException {
        log.info("Происходит обновление вещи по ID: {}", itemId);

        Optional<Item> itemOpt = itemRepository.findById(itemId);

        if (itemOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Вещь для обновления не найдена по ID: " + itemId);
        }

        Item existingItem = itemOpt.get();

        checkUserRightToUpdateItem(ownerId, existingItem);

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
        return toItemResponse(updatedItem);
    }

    public ItemResponse getItemResponseByIdFromUser(Long userId, Long itemId) throws ResponseStatusException {
        log.info("Возращение вещи по ID: {}", itemId);

        boolean isExistUser = userService.existsUser(userId);
        if (!isExistUser) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь по ID: " + userId);
        }

        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдена вещь по ID: " + itemId);
        }

        Item foundItem = itemOpt.get();
        User owner = foundItem.getOwner();
        Long ownerId = owner.getId();

        boolean userIsOwner = ownerId.equals(userId);
        if (userIsOwner) {
            return toItemResponseForOwner(foundItem);
        }

        return toItemResponse(foundItem);
    }

    public Item getItemById(Long itemId) throws ResponseStatusException {
        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдена вещь по ID:" + itemId);
        }

        return itemOpt.get();
    }

    public List<ItemResponse> getAllItemsFromUser(Long userId, int from, int size) {
        boolean isExistsUser = userService.existsUser(userId);
        if (!isExistsUser) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Владелец вещей по ID: " + userId + " не найден в базе данных для возврате всех его вещей");
        }

        MyPageRequest pageRequest = new MyPageRequest(from, size);
        Page<Item> page = itemRepository.findAllByOwnerIdOrderByIdAsc(pageRequest, userId);
        List<Item> itemsFromUser = page.getContent();

        List<ItemResponse> itemsDtoFromUser = new ArrayList<>();
        for (Item item : itemsFromUser) {
            ItemResponse itemResponse = toItemResponseForOwner(item);
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
            ItemResponse itemResponse = toItemResponse(item);
            suitableItemsDto.add(itemResponse);
        }

        return suitableItemsDto;
    }

    public CommentResponse addComment(Long authorId, Long itemId, String textComment) throws ResponseStatusException {

        boolean isExistsUser = userService.existsUser(authorId);
        if (!isExistsUser) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "При добавлении комментария вещи по ID: " + itemId + " не найден пользователь с ID: " + authorId);
        }

        boolean isExistItem = itemRepository.existsById(itemId);
        if (!isExistItem) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Не найдена вещь по ID: " + itemId + ", была попытка добавить комментарий пользователем по ID: " + authorId);
        }

        boolean checkUserRental = checkUserRentalHistory(authorId, itemId);
        if (!checkUserRental) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Пользователь по ID: " + authorId + " не имеет право добавить комментарий вещи по ID: " + itemId + ", так как не брал и не завершил аренду этого предмета");
        }

        Comment comment = toComment(authorId, itemId, textComment);
        comment.setCreated(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        return toCommentResponse(savedComment);
    }

    private boolean checkUserRentalHistory(Long authorId, Long itemId) {
        boolean isRental = bookingRepository.findPastByBookerIdAndItemId(authorId, itemId);

        if (isRental) {
            return true;
        } else {
            return false;
        }
    }

    private void checkUserRightToUpdateItem(Long userId, Item item) throws ResponseStatusException {

        Long itemId = item.getId();
        User owner = item.getOwner();
        Long ownerId = owner.getId();
        boolean isUserOwner = userId.equals(ownerId);
        boolean isExistsUser = userService.existsUser(userId);

        if (!isExistsUser) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Владелец вещи по ID:" + userId + " не найден в базе данных при обновлении вещи по ID:" + itemId);
        }

        if (!isUserOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Пользователь по ID:" + userId + " не владелец вещи по ID:" + itemId);
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
        Boolean available = item.getAvailable();
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

    private ItemResponse toItemResponseForOwner(Item item) {

        Long id = item.getId();
        String name = item.getName();
        String description = item.getDescription();
        Optional<Booking> lastBooking = bookingRepository.findLastBookingByItemId(id);
        Optional<Booking> nextBooking = bookingRepository.findNextBookingByItemId(id);
        Boolean available = item.getAvailable();
        List<Comment> comments = item.getComments();
        ItemRequest request = item.getRequest();
        Long requestId;

        BookingRes lastBookingRes = toBookingRes(lastBooking);
        BookingRes nextBookingRes = toBookingRes(nextBooking);

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
        response.setLastBooking(lastBookingRes);
        response.setNextBooking(nextBookingRes);
        response.setAvailable(available);
        response.setRequestId(requestId);
        response.setComments(commentResponseList);

        return response;
    }

    private BookingRes toBookingRes(Optional<Booking> bookingOpt) {
        if (bookingOpt.isEmpty()) {
            return null;
        }

        Booking booking = bookingOpt.get();

        User booker = booking.getBooker();
        Long bookerId = booker.getId();
        Long id = booking.getId();

        BookingRes bookingRes = new BookingRes();
        bookingRes.setId(id);
        bookingRes.setBookerId(bookerId);

        return bookingRes;
    }

    private Comment toComment(Long authorId, Long itemId, String textComment) {
        Item item = getItemById(itemId);
        User author = userService.getUserById(authorId);

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