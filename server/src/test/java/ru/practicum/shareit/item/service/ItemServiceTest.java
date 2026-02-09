package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.dto.CommentResponse;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.item.model.dto.ItemResponse;
import ru.practicum.shareit.item.storage.CommentRepository;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ItemRequestService itemRequestService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ItemService itemService;

    private ItemDto itemDto;

    private ItemResponse itemResponse;

    private User owner;

    @Captor
    ArgumentCaptor<Item> itemCaptor;

    @Captor
    ArgumentCaptor<Comment> commentCaptor;

    @BeforeEach
    void setUp() {
        itemDto = new ItemDto();

        owner = new User();
        owner.setId(1L);
    }

    @Test
    void add_whenRequestValid_whenSavedItem() {
        //given
        Long ownerId = owner.getId();

        when(userService.existsUser(ownerId)).thenReturn(true);
        when(userService.getUserById(ownerId)).thenReturn(owner);
        when(itemRepository.save(any()))
                .thenAnswer(i ->
                {
                    Item save = i.getArgument(0);
                    save.setId(1L);
                    return save;
                });

        //when
        ItemResponse res = itemService.add(ownerId, itemDto);

        //then
        //ожидаемое значение
        Long itemId = 1L;

        //
        assertEquals(itemId, res.getId());
        verify(userService).existsUser(ownerId);
        verify(userService).getUserById(ownerId);
        verify(itemRepository).save(any());
    }

    @Test
    void add_whenUserNotFound_whenThrowResourceNotFoundException() {
        //given
        Long nonExitsOwnerId = owner.getId(); //ID не существующего пользователя

        when(userService.existsUser(nonExitsOwnerId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.add(nonExitsOwnerId, itemDto));

        //then
        //ожидаемое значение
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Пользователь по ID: " + nonExitsOwnerId + " не найден для сохранения вещи: " + itemDto + "\"";

        //проверка
        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userService).existsUser(nonExitsOwnerId);
    }

    @Test
    void updateItem_whenRequestValid_whenUpdateItem() {
        //given
        //параметры для запроса
        itemDto.setName("Updated Name");
        itemDto.setDescription("Updated Description");
        itemDto.setAvailable(false);

        Long ownerId = owner.getId();
        Long itemId = 1L;

        when(itemRepository.findById(itemId))
                .thenAnswer(i -> { // симуляция, что с бд приходит Item с запрашиваемым ID
                    Long id = i.getArgument(0);

                    Item save = new Item();
                    save.setId(id);
                    save.setOwner(owner); //у вещи должен быть хозяин

                    return Optional.of(save);
                });
        when(userService.existsUser(ownerId)).thenReturn(true);
        when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0)); //типа сохранили обновленный Item в бд и вернули его

        //when
        ItemResponse resItem = itemService.updateItem(ownerId, itemId, itemDto);

        //then
        //ожидаемые значения
        Long itemIdExpected = itemId;
        String itemNameExpected = itemDto.getName();
        String itemDescriptionExpected = itemDto.getDescription();
        Boolean availableExpected = itemDto.getAvailable();
        Long ownerIdExpected = owner.getId();

        // ловим Item который сохраняем в бд
        verify(itemRepository).save(itemCaptor.capture());
        Item saveItem = itemCaptor.getValue();

        User ownerSaved = saveItem.getOwner();


        //проверяем saveItem
        assertEquals(itemIdExpected, saveItem.getId());
        assertEquals(itemNameExpected, saveItem.getName());
        assertEquals(itemDescriptionExpected, saveItem.getDescription());
        assertEquals(availableExpected, saveItem.getAvailable());
        assertEquals(ownerIdExpected, ownerSaved.getId());

        //проверяем resItem
        assertEquals(itemIdExpected, resItem.getId());
        assertEquals(itemNameExpected, resItem.getName());
        assertEquals(itemDescriptionExpected, resItem.getDescription());
        assertEquals(availableExpected, resItem.getAvailable());

        verify(itemRepository).findById(itemId);
        verify(userService).existsUser(ownerId);
        verify(itemRepository).save(any());
    }

    @Test
    void updateItem_whenItemNotFound_whenThrowResourceNotFoundException() {
        //given
        //параметры для запроса
        itemDto.setName("Updated Name");
        itemDto.setDescription("Updated Description");
        itemDto.setAvailable(false);

        Long ownerId = owner.getId();
        Long nonExistItemId = 777L; //несуществующий ID вещи

        when(itemRepository.findById(nonExistItemId)).thenReturn(Optional.empty());

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.updateItem(ownerId, nonExistItemId, itemDto));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Вещь для обновления не найдена по ID: " + nonExistItemId + "\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(itemRepository).findById(nonExistItemId);
    }

    @Test
    void updateItem_whenUserNotFound_whenThrowResourceNotFoundException() {
        //given
        //параметры для запроса
        itemDto.setName("Updated Name");
        itemDto.setDescription("Updated Description");
        itemDto.setAvailable(false);

        Long nonExistUserId = 777L; //несуществующий пользователь, который производит запрос на обновление
        Long itemId = 1L;

        when(itemRepository.findById(itemId))
                .thenAnswer(i -> { // симуляция, что с бд приходит Item с запрашиваемым ID
                    Long id = i.getArgument(0);

                    Item save = new Item();
                    save.setId(id);
                    save.setOwner(owner); //у вещи должен быть хозяин

                    return Optional.of(save);
                });
        when(userService.existsUser(nonExistUserId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.updateItem(nonExistUserId, itemId, itemDto));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Владелец вещи по ID:" + nonExistUserId + " не найден в базе данных при обновлении вещи по ID:" + itemId + "\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(itemRepository).findById(itemId);
        verify(userService).existsUser(nonExistUserId);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_whenUserIsItemNonOwner_whenThrowResourceNotFoundException() {
        //given
        //параметры для запроса
        itemDto.setName("Updated Name");
        itemDto.setDescription("Updated Description");
        itemDto.setAvailable(false);

        Long requestingUser = 777L; // пользователь, который производит запрос на обновление, но не является хозяином вещи
        Long ownerId = owner.getId(); //пользователь хозяин вещи
        Long itemId = 1L;

        when(itemRepository.findById(itemId))
                .thenAnswer(i -> { // симуляция, что с бд приходит Item с запрашиваемым ID
                    Long id = i.getArgument(0);

                    Item save = new Item();
                    save.setId(id);
                    save.setOwner(owner); //у вещи должен быть хозяин

                    return Optional.of(save);
                });
        when(userService.existsUser(requestingUser)).thenReturn(true);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.updateItem(requestingUser, itemId, itemDto));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.FORBIDDEN;
        String exceptionMessage = "403 FORBIDDEN \"Пользователь по ID:" + requestingUser + " не владелец вещи по ID:" + itemId + "\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(itemRepository).findById(itemId);
        verify(userService).existsUser(requestingUser);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void getItemResponseByIdFromUser_whenRequestUserIsItemOwner_thenReturnItemResponseForOwner() {
        //given
        //Эти поля нужны для возврата вещи его владельцу, владельцу видны след и предыдущие бронирования его вещи
        User bookerLast = new User();
        bookerLast.setId(2L);
        User bookerNext = new User();
        bookerNext.setId(3L);

        Booking bookingLast = new Booking();
        bookingLast.setId(2L);
        bookingLast.setBooker(bookerLast);
        Booking bookingNext = new Booking();
        bookingNext.setId(3L);
        bookingNext.setBooker(bookerNext);

        //поля для запроса
        Long ownerId = owner.getId();
        Long itemId = 1L;

        when(userService.existsUser(ownerId)).thenReturn(true);
        when(itemRepository.findById(itemId))
                .thenAnswer(i -> { // симуляция, что с бд приходит Item с запрашиваемым ID
                    Long id = i.getArgument(0);

                    Item item = new Item();
                    item.setId(id);
                    item.setOwner(owner); //у вещи должен быть хозяин

                    return Optional.of(item);
                });
        when(bookingRepository.findLastBookingByItemId(itemId)).thenReturn(Optional.of(bookingLast));
        when(bookingRepository.findNextBookingByItemId(itemId)).thenReturn(Optional.of(bookingNext));
        //when
        ItemResponse itemRes = itemService.getItemResponseByIdFromUser(ownerId, itemId);

        //then
        //ожидаемые значения
        Long idItemExpected = itemId;
        Long idBookingLastExpected = bookingLast.getId();
        Long idBookerLastExpected = bookerLast.getId();
        Long idBookingNextExpected = bookingNext.getId();
        Long idBookerNextExpected = bookerNext.getId();

        //проверка itemRes
        ItemResponse.BookingRes lastBooking = itemRes.getLastBooking();
        ItemResponse.BookingRes nextBooking = itemRes.getNextBooking();

        assertEquals(idItemExpected, itemRes.getId());
        assertEquals(idBookingLastExpected, lastBooking.getId());
        assertEquals(idBookerLastExpected, lastBooking.getBookerId());
        assertEquals(idBookingNextExpected, nextBooking.getId());
        assertEquals(idBookerNextExpected, nextBooking.getBookerId());
        verify(userService).existsUser(ownerId);
        verify(itemRepository).findById(itemId);
        verify(bookingRepository).findLastBookingByItemId(itemId);
        verify(bookingRepository).findNextBookingByItemId(itemId);
    }

    @Test
    void getItemResponseByIdFromUser_whenRequestUserIsNotItemOwner_thenReturnItemResponseForUser() {
        //given
        //поля для запроса
        Long userId = 45L; //когда запрашивающий пользователь не владелец вещи
        Long itemId = 1L;

        when(userService.existsUser(userId)).thenReturn(true);
        when(itemRepository.findById(itemId))
                .thenAnswer(i -> { // симуляция, что с бд приходит Item с запрашиваемым ID
                    Long id = i.getArgument(0);

                    Item item = new Item();
                    item.setId(id);
                    item.setOwner(owner); //у вещи должен быть хозяин

                    return Optional.of(item);
                });

        //when
        ItemResponse itemRes = itemService.getItemResponseByIdFromUser(userId, itemId);

        //then
        ItemResponse.BookingRes lastBooking = itemRes.getLastBooking();
        ItemResponse.BookingRes nextBooking = itemRes.getNextBooking();

        assertEquals(itemId, itemRes.getId());
        assertNull(lastBooking);
        assertNull(nextBooking);
        verify(userService).existsUser(userId);
        verify(itemRepository).findById(itemId);
        verify(bookingRepository, never()).findLastBookingByItemId(itemId);
        verify(bookingRepository, never()).findNextBookingByItemId(itemId);
    }

    @Test
    void getItemResponseByIdFromUser_whenRequestUserIsNotFound_thenThrowResourceNotFoundException() {
        //given
        //поля для запроса
        Long notExistUserId = 777L; //когда запрашивающий пользователя не существует
        Long itemId = 1L;

        when(userService.existsUser(notExistUserId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.getItemResponseByIdFromUser(notExistUserId, itemId));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найден пользователь по ID: " + notExistUserId + "\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userService).existsUser(notExistUserId);
        verify(itemRepository, never()).findById(itemId);
    }

    @Test
    void getItemResponseByIdFromUser_whenItemNotFound_thenThrowResourceNotFoundException() {
        //given
        //поля для запроса
        Long userId = owner.getId();
        Long notExistItemId = 1L; // запрашиваемая несуществующая вещь

        when(userService.existsUser(userId)).thenReturn(true);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.getItemResponseByIdFromUser(userId, notExistItemId));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найдена вещь по ID: " + notExistItemId + "\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(userService).existsUser(userId);
        verify(itemRepository).findById(notExistItemId);
    }

    @Test
    void getItemById_whenItemFound_thenReturnItem() {
        //given
        Long itemId = 1L;

        Item item = new Item();
        item.setId(itemId);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        //when
        Item result = itemService.getItemById(itemId);

        //then
        assertEquals(itemId, result.getId());
        assertEquals(item, result);
        verify(itemRepository).findById(itemId);
    }

    @Test
    void getItemById_whenItemNotFound_thenThrowResponseStatusException() {
        //given
        Long notExistItemId = 777L;

        when(itemRepository.findById(notExistItemId)).thenReturn(Optional.empty());

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.getItemById(notExistItemId));

        //then
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найдена вещь по ID:" + notExistItemId + "\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());
        verify(itemRepository).findById(notExistItemId);
    }

    @Test
    void getAllItemsFromUser_whenRequestingUserHasHisOwnThings_thenReturnNonEmptyList() {
        //given
        //Эти поля нужны для возврата вещи его владельцу, владельцу видны след и предыдущие бронирования его вещи
        User bookerLast = new User();
        bookerLast.setId(2L);
        User bookerNext = new User();
        bookerNext.setId(3L);

        Booking bookingLast = new Booking();
        bookingLast.setId(2L);
        bookingLast.setBooker(bookerLast);
        Booking bookingNext = new Booking();
        bookingNext.setId(3L);
        bookingNext.setBooker(bookerNext);

        //параметры для запроса
        int from = 0;
        int size = 10;
        Long userId = owner.getId();


        when(userService.existsUser(userId)).thenReturn(true);
        when(itemRepository.findAllByOwnerIdOrderByIdAsc(any(), anyLong()))
                .thenAnswer(i -> {
                    Item item = new Item();
                    item.setId(77L);
                    item.setOwner(owner);

                    List<Item> items = List.of(item);

                    Page<Item> page = new PageImpl<>(items);
                    return page;
                });
        when(bookingRepository.findLastBookingByItemId(anyLong())).thenReturn(Optional.of(bookingLast));
        when(bookingRepository.findNextBookingByItemId(anyLong())).thenReturn(Optional.of(bookingNext));

        //when
        List<ItemResponse> result = itemService.getAllItemsFromUser(userId, from, size);

        //then
        //ожидаемые значения
        Long idItemExpected = 77L;
        Long idBookingLastExpected = bookingLast.getId();
        Long idBookerLastExpected = bookerLast.getId();
        Long idBookingNextExpected = bookingNext.getId();
        Long idBookerNextExpected = bookerNext.getId();

        //проверка result
        ItemResponse itemRes = result.get(0);
        ItemResponse.BookingRes lastBooking = itemRes.getLastBooking();
        ItemResponse.BookingRes nextBooking = itemRes.getNextBooking();

        assertEquals(idItemExpected, itemRes.getId());
        assertEquals(idBookingLastExpected, lastBooking.getId());
        assertEquals(idBookerLastExpected, lastBooking.getBookerId());
        assertEquals(idBookingNextExpected, nextBooking.getId());
        assertEquals(idBookerNextExpected, nextBooking.getBookerId());
        assertEquals(1, result.size());

        verify(userService).existsUser(userId);
        verify(itemRepository).findAllByOwnerIdOrderByIdAsc(any(), anyLong());
        verify(bookingRepository).findLastBookingByItemId(anyLong());
        verify(bookingRepository).findNextBookingByItemId(anyLong());
    }

    @Test
    void getAllItemsFromUser_whenRequestingUserNotHasHisOwnThings_thenReturnEmptyList() {
        //given
        //параметры для запроса
        int from = 0;
        int size = 10;
        Long userId = owner.getId();

        when(userService.existsUser(userId)).thenReturn(true);
        when(itemRepository.findAllByOwnerIdOrderByIdAsc(any(), anyLong()))
                .thenAnswer(i -> {

                    List<Item> items = List.of();

                    Page<Item> page = new PageImpl<>(items);
                    return page;
                });

        //when
        List<ItemResponse> result = itemService.getAllItemsFromUser(userId, from, size);

        //then
        assertTrue(result.isEmpty());

        verify(userService).existsUser(userId);
        verify(itemRepository).findAllByOwnerIdOrderByIdAsc(any(), anyLong());
        verify(bookingRepository, never()).findLastBookingByItemId(anyLong());
        verify(bookingRepository, never()).findNextBookingByItemId(anyLong());
    }

    @Test
    void getAllItemsFromUser_whenUserNotFound_thenThrowResponseStatusException() {
        //given
        int from = 0;
        int size = 10;
        Long nonExistUserId = 7777L;

        when(userService.existsUser(nonExistUserId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.getAllItemsFromUser(nonExistUserId, from, size));

        //
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String message = "404 NOT_FOUND \"Владелец вещей по ID: " + nonExistUserId + " не найден в базе данных для возврате всех его вещей\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(message, resException.getMessage());
        verify(userService).existsUser(nonExistUserId);
        verify(itemRepository, never()).findAllByOwnerIdOrderByIdAsc(any(), anyLong());
        verify(bookingRepository, never()).findLastBookingByItemId(anyLong());
        verify(bookingRepository, never()).findNextBookingByItemId(anyLong());
    }

    @Test
    void itemSearch_whenTextEmpty_thenReturnEmptyList() {
        //given
        String text = ""; //пустая строка для поиска вещи
        int from = 0;
        int size = 10;
        Long userId = owner.getId();

        when(userService.existsUser(userId)).thenReturn(true);

        //when
        List<ItemResponse> result = itemService.itemSearch(text, userId, from, size);

        //then
        assertTrue(result.isEmpty());
        verify(userService).existsUser(userId);
    }

    @Test
    void itemSearch_whenTextNotEmpty_thenReturnNotEmptyList() {
        //given
        String text = "Отвертка"; //не пустая строка для поиска вещи
        int from = 0;
        int size = 10;
        Long userId = owner.getId();

        when(userService.existsUser(userId)).thenReturn(true);
        when(itemRepository.findAvailableItemsBySearchText(any(), anyString()))
                .thenAnswer(i ->
                {
                    String textSearch = i.getArgument(1);

                    Item item = new Item();
                    item.setName(textSearch); // нашли совпадение в имени вещи
                    item.setId(77L);
                    item.setOwner(owner);

                    List<Item> items = List.of(item);

                    Page<Item> page = new PageImpl<>(items);
                    return page;
                });

        //when
        List<ItemResponse> result = itemService.itemSearch(text, userId, from, size);

        //then
        //ожидаемые значения
        Long idItemExpected = 77L;
        String nameItemExpected = text;

        //проверяем result
        ItemResponse itemRes = result.get(0);
        ItemResponse.BookingRes lastBooking = itemRes.getLastBooking();
        ItemResponse.BookingRes nextBooking = itemRes.getNextBooking();
        List<CommentResponse> commentResponseList = itemRes.getComments();

        assertEquals(idItemExpected, itemRes.getId());
        assertEquals(nameItemExpected, itemRes.getName());
        assertNull(lastBooking);
        assertNull(nextBooking);
        assertTrue(commentResponseList.isEmpty());
        assertEquals(1L, result.size());

        verify(userService).existsUser(userId);
        verify(itemRepository).findAvailableItemsBySearchText(any(), anyString());
        verify(bookingRepository, never()).findLastBookingByItemId(anyLong());
        verify(bookingRepository, never()).findNextBookingByItemId(anyLong());
    }

    @Test
    void itemSearch_whenUserNotFound_thenReturnEmptyList() {
        //given
        String text = "Отвертка"; //не пустая строка для поиска вещи
        int from = 0;
        int size = 10;
        Long nonExistUserId = 777L;

        when(userService.existsUser(nonExistUserId)).thenReturn(false);

        //when
        List<ItemResponse> result = itemService.itemSearch(text, nonExistUserId, from, size);

        //then
        assertTrue(result.isEmpty());
        verify(userService).existsUser(nonExistUserId);
        verify(itemRepository, never()).findAvailableItemsBySearchText(any(), anyString());
        verify(bookingRepository, never()).findLastBookingByItemId(anyLong());
        verify(bookingRepository, never()).findNextBookingByItemId(anyLong());
    }


    @Test
    void addComment_whenRequestValid_thenSaveComment() {
        //given
        String commentText = "Комментарий какой-то";
        Long authorId = owner.getId();
        Long itemId = 1L;

        when(userService.existsUser(authorId)).thenReturn(true);
        when(itemRepository.existsById(itemId)).thenReturn(true);

        //тут сделали что пользователь брал в аренду этот предмет
        when(bookingRepository.findPastByBookerIdAndItemId(anyLong(), anyLong())).thenReturn(List.of(new Booking()));

        when(itemRepository.findById(itemId))
                .thenAnswer(i -> {
                    Item item = new Item();
                    item.setId(itemId);

                    return Optional.of(item);
                });

        when(userService.getUserById(anyLong()))
                .thenAnswer(i ->
                {
                    owner.setName("Имя автора комментария");// у автора должно быть имя
                    return owner;
                });

        when(commentRepository.save(any()))
                .thenAnswer(i ->
                {
                    Comment comment = i.getArgument(0);
                    comment.setId(1L);
                    return comment;
                });

        //when
        CommentResponse response = itemService.addComment(authorId, itemId, commentText);

        //then
        //ожидаемые значения
        Long commentIdExpected = 1L;
        Long authorIdExpected = owner.getId();
        String authorNameExpected = owner.getName();
        Long itemIdExpected = itemId;
        String commentTextExpected = commentText;

        //для проверки ловим коммент который сохраняется в бд
        verify(commentRepository).save(commentCaptor.capture());
        Comment savedComment = commentCaptor.getValue();
        User authorSaved = savedComment.getAuthor();
        Item itemSaved = savedComment.getItem();

        //проверка savedComment
        assertEquals(commentIdExpected, savedComment.getId());
        assertEquals(authorIdExpected, authorSaved.getId());
        assertEquals(authorNameExpected, authorSaved.getName());
        assertEquals(itemIdExpected, itemSaved.getId());
        assertEquals(commentTextExpected, savedComment.getText());
        assertNotNull(savedComment.getCreated());

        //проверка response
        assertEquals(commentIdExpected, response.getId());
        assertEquals(authorNameExpected, response.getAuthorName());
        assertEquals(commentTextExpected, response.getText());
        assertNotNull(response.getCreated());

        verify(userService).existsUser(authorId);
        verify(itemRepository).existsById(itemId);
        verify(bookingRepository).findPastByBookerIdAndItemId(anyLong(), anyLong());
        verify(itemRepository).findById(itemId);
        verify(userService).getUserById(authorId);
        verify(commentRepository).save(savedComment);
    }

    @Test
    void addComment_whenUserNotFound_thenThrowResponseStatusException() {
        //given
        String commentText = "Комментарий какой-то";
        Long notExistAuthorId = 77L; //несуществующий пользователь
        Long itemId = 1L;

        when(userService.existsUser(notExistAuthorId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.addComment(notExistAuthorId, itemId, commentText));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"При добавлении комментария вещи по ID: " + itemId + " не найден пользователь с ID: " + notExistAuthorId + "\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());

        verify(userService).existsUser(notExistAuthorId);
        verify(itemRepository, never()).existsById(anyLong());
        verify(bookingRepository, never()).findPastByBookerIdAndItemId(anyLong(), anyLong());
        verify(itemRepository, never()).findById(anyLong());
        verify(userService, never()).getUserById(anyLong());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_whenItemNotFound_thenThrowResponseStatusException() {
        //given
        String commentText = "Комментарий какой-то";
        Long authorId = owner.getId();
        Long notExistItemId = 77L; //несуществующая вещь

        when(userService.existsUser(authorId)).thenReturn(true);
        when(itemRepository.existsById(notExistItemId)).thenReturn(false);

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.addComment(authorId, notExistItemId, commentText));

        //then
        //ожидаемые значения
        HttpStatus status = HttpStatus.NOT_FOUND;
        String exceptionMessage = "404 NOT_FOUND \"Не найдена вещь по ID: " + notExistItemId + ", была попытка добавить комментарий пользователем по ID: " + authorId + "\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());

        verify(userService).existsUser(authorId);
        verify(itemRepository).existsById(notExistItemId);
        verify(bookingRepository, never()).findPastByBookerIdAndItemId(anyLong(), anyLong());
        verify(itemRepository, never()).findById(anyLong());
        verify(userService, never()).getUserById(anyLong());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_whenUserNotRentedItemAndWantsToAddComment_thenThrowResponseStatusException() {
        //given
        String commentText = "Комментарий какой-то";
        Long authorId = owner.getId(); //пользователь, который не брал товар в аренду
        Long itemId = 1L;

        when(userService.existsUser(authorId)).thenReturn(true);
        when(itemRepository.existsById(itemId)).thenReturn(true);
        when(bookingRepository.findPastByBookerIdAndItemId(authorId, itemId)).thenReturn(List.of());// не нашли бронирования этой вещи пользователем

        //when
        ResponseStatusException resException = assertThrows(ResponseStatusException.class, () -> itemService.addComment(authorId, itemId, commentText));

        //then
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String exceptionMessage
                = "400 BAD_REQUEST \"Пользователь по ID: " + authorId + " не имеет право добавить комментарий вещи по ID: " + itemId
                + ", так как не брал и не завершил аренду этого предмета\"";

        assertEquals(status, resException.getStatusCode());
        assertEquals(exceptionMessage, resException.getMessage());

        verify(userService).existsUser(authorId);
        verify(itemRepository).existsById(itemId);
        verify(bookingRepository).findPastByBookerIdAndItemId(authorId, itemId);
        verify(itemRepository, never()).findById(anyLong());
        verify(userService, never()).getUserById(anyLong());
        verify(commentRepository, never()).save(any());
    }
}