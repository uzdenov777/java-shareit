package ru.practicum.shareit.request.storage;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.util.MyPageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ItemRequestRepositoryIT {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private ItemRequest itemRequest;

    private ItemRequest anotherRequest;

    private User requestor;

    private User anotherRequestor;

    private Long requestorId;
    private Long anotherRequestorId;
    private MyPageRequest pageRequest;

    @BeforeEach
    void setUp() {
        requestor = new User();
        requestor.setName("nameUser");
        requestor.setEmail("emailUser@mail.com");

        anotherRequestor = new User();
        anotherRequestor.setName("AnotherNameUser");
        anotherRequestor.setEmail("emailAnother@gmail.com");

        entityManager.persist(requestor);
        entityManager.persist(anotherRequestor);
        requestorId = requestor.getId();
        anotherRequestorId = anotherRequestor.getId();

        itemRequest = new ItemRequest();
        itemRequest.setRequestor(requestor);
        itemRequest.setDescription("description");
        itemRequest.setCreated(LocalDateTime.now());

        anotherRequest = new ItemRequest();
        anotherRequest.setRequestor(anotherRequestor);
        anotherRequest.setDescription("descriptionAnother");
        anotherRequest.setCreated(LocalDateTime.now());

        entityManager.persist(itemRequest);
        entityManager.persist(anotherRequest);

        pageRequest = new MyPageRequest(0, 10);
    }

    @AfterEach
    void clear() {
        entityManager.clear();
    }

    @Test
    void findByRequestorId_whenRequestorHasExistRequest_thenReturnNonEmptyList() {
        //when
        List<ItemRequest> response = itemRequestRepository.findByRequestorId(requestorId);

        //then
        ItemRequest resultItemReq = response.get(0);

        assertEquals(itemRequest, resultItemReq);
        assertEquals(1, response.size());
    }

    @Test
    void findByRequestorId_whenRequestorHasNoRequests_thenReturnEmptyList() {
        //given
        //теперь у Requestor нет запросов
        entityManager.remove(itemRequest);

        //when
        List<ItemRequest> response = itemRequestRepository.findByRequestorId(requestorId);

        //then
        assertTrue(response.isEmpty());
    }

    @Test
    void findByRequestorIdNot_whenOtherUsersHaveRequests_thenReturnNotEmptyList() {
        //when
        Page<ItemRequest> response = itemRequestRepository.findByRequestorIdNot(requestorId, pageRequest);

        //then
        List<ItemRequest> itemRequests = response.getContent();
        ItemRequest resultItemReq = itemRequests.get(0);

        assertEquals(anotherRequest, resultItemReq);
        assertEquals(1, itemRequests.size());
    }

    @Test
    void findByRequestorIdNot_whenOtherUsersNoHaveRequests_thenReturnEmptyList() {
        //given
        //удалили запросы других пользователей, в бд остались запросы только пользователя Requestor
        entityManager.remove(anotherRequest);

        //when
        Page<ItemRequest> response = itemRequestRepository.findByRequestorIdNot(requestorId, pageRequest);

        //then
        List<ItemRequest> itemRequests = response.getContent();

        assertTrue(itemRequests.isEmpty());
    }
}