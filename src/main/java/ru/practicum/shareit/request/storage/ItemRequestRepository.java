package ru.practicum.shareit.request.storage;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.util.MyPageRequest;
import ru.practicum.shareit.user.model.User;

import java.util.List;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {
    List<ItemRequest> findByRequestor(User requestor);

    @Query
    Page<ItemRequest> findByRequestorNot(MyPageRequest pageRequest,  User requestor);
}