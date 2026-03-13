package ru.practicum.shareit.request.storage;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.util.MyPageRequest;

import java.util.List;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {

    List<ItemRequest> findByRequestorId(Long requestorId);

    Page<ItemRequest> findByRequestorIdNot(Long requestorId, MyPageRequest pageRequest);
}