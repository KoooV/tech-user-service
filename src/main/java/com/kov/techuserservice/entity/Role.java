package com.kov.techuserservice.entity;

import com.kov.techuserservice.dto.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Сущность роли пользователя в системе.
 * Определяет набор разрешений и уровень доступа.
 * Привязана к пользователям через Many-to-Many связь и к разрешениям через Many-to-Many.
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    /** Уникальный идентификатор роли (первичный ключ). Автоматически генерируется. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Название роли, определяющее уровень доступа. Перечисление: USER, MANAGER, ADMIN. Значение хранится как строка в БД. Уникально. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleName name;

    /** Описание роли — текстовое пояснение её назначения и полномочий (например: "Обычный пользователь с доступом к каталогу и заказам"). */
    private String description;

    /** Дата и время создания роли в системе. Устанавливается автоматически при первом сохранении. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Множество разрешений (permissions), входящих в данную роль. Определяет конкретные операции, которые может выполнять пользователь с этой ролью. Загружается немедленно (EAGER) для корректной работы Spring Security. */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    /** Колбэк, вызываемый перед сохранением сущности в БД. Устанавливает дату создания. */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
