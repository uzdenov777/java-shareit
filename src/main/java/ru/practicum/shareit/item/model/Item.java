package ru.practicum.shareit.item.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * TODO Sprint add-controllers.
 */

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    @NotBlank(message = "Название  у вещи не может отсутствовать")
    private String name;

    @Column(name = "description")
    @NotBlank(message = "Описание у вещи не может отсутствовать")
    private String description;

    @Column(name = "available")
    @NotNull(message = "Не может отсутствовать статус у Вещи")
    private Boolean available;

    @Column(name = "owner_id")
    private Long ownerId;
}