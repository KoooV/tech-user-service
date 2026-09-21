package com.kov.techuserservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Сущность разрешения (permission) в системе.
 * Определяет конкретное действие, которое может выполнять пользователь.
 * Входит в состав ролей через Many-to-Many связь.
 */
@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    /** Уникальный идентификатор разрешения (первичный ключ). Автоматически генерируется. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Наименование разрешения в строковом виде (например: "ORDER_CREATE", "USER_READ", "PRODUCT_DELETE"). Определяет конкретную операцию, доступную пользователю с данным разрешением. Уникально. */
    @Column(nullable = false)
    private String name;

    /** Описание разрешения — текстовое пояснение того, какую операцию позволяет выполнить данное разрешение. */
    private String description;
}
