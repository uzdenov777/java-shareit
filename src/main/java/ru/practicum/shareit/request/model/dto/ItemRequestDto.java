package ru.practicum.shareit.request.model.dto;

import lombok.Data;
import ru.practicum.shareit.item.model.dto.ItemDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItemRequestDto {

    private String description;

    private LocalDateTime creationDate;

    private List<ItemDto> responseItems = new ArrayList<>();
}