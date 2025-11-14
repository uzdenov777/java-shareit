package ru.practicum.shareit.request.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.dto.ItemDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.model.dto.ItemRequestDto;
import ru.practicum.shareit.request.storage.ItemRequestRepository;
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
public class ItemRequestService {

    private final UserService userService;
    private final ItemRequestRepository itemRequestRepository;

    @Autowired
    public ItemRequestService(UserService userService, ItemRequestRepository itemRequestRepository) {
        this.userService = userService;
        this.itemRequestRepository = itemRequestRepository;
    }

    public ItemRequest create(Long requestorId, ItemRequest itemRequest) {

        User requestor = userService.getUserById(requestorId);
        LocalDateTime creationDate = LocalDateTime.now();

        itemRequest.setRequestor(requestor);
        itemRequest.setCreated(creationDate);

        ItemRequest saveItemRequest = itemRequestRepository.save(itemRequest);

        return saveItemRequest;
    }

    public ItemRequest getItemRequestById(Long itemRequestId) throws ResponseStatusException {
        Optional<ItemRequest> itemRequestOptional = itemRequestRepository.findById(itemRequestId);

        if (itemRequestOptional.isEmpty()) {
            log.info("По ID:{} запрос на вещь не найден", itemRequestId);
            return null;
        }

        ItemRequest itemRequest = itemRequestOptional.get();

        return itemRequest;
    }

    public List<ItemRequestDto> getByRequestor(Long bookerId) {

        User requestor = userService.getUserById(bookerId);

        List<ItemRequest> userRequests = itemRequestRepository.findByRequestor(requestor);
        List<ItemRequestDto> userRequestsDto = toItemRequestDtoList(userRequests);

        return userRequestsDto;
    }

    public List<ItemRequestDto> getAll(int from, int size, Long requestorId) {
        User requestor = userService.getUserById(requestorId);

        Sort sort = Sort.by(Sort.Direction.DESC, "created");
        MyPageRequest pageRequest = new MyPageRequest(from, size, sort);
        Page<ItemRequest> page = itemRequestRepository.findByRequestorNot(pageRequest, requestor);
        List<ItemRequest> itemRequests = page.getContent();
        List<ItemRequestDto> itemRequestsDto = toItemRequestDtoList(itemRequests);
        return itemRequestsDto;
    }

    public ItemRequestDto getItemRequestDtoById(Long requestId, Long userId) throws ResponseStatusException {
        boolean isExistsUser = userService.existsUser(userId);
        if (!isExistsUser) {
            log.info("Не найден пользователь по ID: {}, для возвращения запроса на вещь по ID: {}", userId, requestId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден пользователь по ID: " + userId + ", для возвращения запроса на вещь по ID: " + requestId);
        }

        Optional<ItemRequest> itemRequestOptional = itemRequestRepository.findById(requestId);
        if (itemRequestOptional.isEmpty()) {
            log.info("Не найден для возвращения по ID: {} запрос на вещи, пользователем по ID: {}", requestId, userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найден для возвращения по ID: " + requestId + " запрос на вещи, пользователем по ID: " + userId);
        }

        ItemRequest itemRequest = itemRequestOptional.get();
        ItemRequestDto itemRequestDto = toItemRequestDto(itemRequest);
        return itemRequestDto;
    }

    private ItemRequestDto toItemRequestDto(ItemRequest itemRequest) {
        Long id = itemRequest.getId();
        String description = itemRequest.getDescription();
        LocalDateTime creationDate = itemRequest.getCreated();
        List<Item> responseItems = itemRequest.getItems();

        ItemRequestDto itemRequestDto = new ItemRequestDto();
        itemRequestDto.setId(id);
        itemRequestDto.setDescription(description);
        itemRequestDto.setCreated(creationDate);
        List<ItemDto> responseItemsDto = toItemsDtoList(responseItems);
        itemRequestDto.setItems(responseItemsDto);

        return itemRequestDto;
    }

    private List<ItemRequestDto> toItemRequestDtoList(List<ItemRequest> itemRequests) {
        List<ItemRequestDto> userRequestsDto = new ArrayList<>();

        for (ItemRequest itemRequest : itemRequests) {

            ItemRequestDto itemRequestDto = toItemRequestDto(itemRequest);

            userRequestsDto.add(itemRequestDto);
        }

        return userRequestsDto;
    }

    private List<ItemDto> toItemsDtoList(List<Item> items) {
        List<ItemDto> itemsDto = new ArrayList<>();

        for (Item item : items) {
            Long id = item.getId();
            String name = item.getName();
            String description = item.getDescription();
            Boolean isAvailable = item.getAvailable();
            ItemRequest itemRequest = item.getRequest();
            Long requestId;

            if (Objects.nonNull(itemRequest)) {
                requestId = itemRequest.getId();
            } else {
                requestId = null;
            }

            ItemDto itemDto = new ItemDto();
            itemDto.setId(id);
            itemDto.setName(name);
            itemDto.setDescription(description);
            itemDto.setAvailable(isAvailable);
            itemDto.setRequestId(requestId);

            itemsDto.add(itemDto);
        }

        return itemsDto;
    }
}