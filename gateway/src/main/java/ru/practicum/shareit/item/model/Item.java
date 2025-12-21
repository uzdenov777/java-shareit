package ru.practicum.shareit.item.model;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO Sprint add-controllers.
 */

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Item {

    private Long id;

    @NotBlank(message = "Название  у вещи не может отсутствовать")
    private String name;

    @NotBlank(message = "Описание у вещи не может отсутствовать")
    private String description;

    @NotNull(message = "Не может отсутствовать статус у Вещи")
    private Boolean available;

    private ItemRequest request;

    private User owner;

    private List<Comment> comments = new ArrayList<>();
}