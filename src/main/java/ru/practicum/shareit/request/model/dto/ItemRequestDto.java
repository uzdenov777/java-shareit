package ru.practicum.shareit.request.model.dto;

import lombok.Data;
import ru.practicum.shareit.item.model.dto.ItemDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItemRequestDto {

    private Long id;

    private String description;

    private LocalDateTime created;

    private List<ItemDto> items = new ArrayList<>();
}