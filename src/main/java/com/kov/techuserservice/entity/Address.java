package com.kov.techuserservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Сущность адреса доставки пользователя.
 * Связана с пользователем по Many-to-One (многие адресов — один пользователь).
 * Хранит данные для доставки заказов.
 */
@Entity
@Table(name = "addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    /** Уникальный идентификатор адреса (первичный ключ). Автоматически генерируется. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Страна доставки. Определяет международные правила отправки и налоговую политику. Не может быть пустым. */
    @Column(nullable = false)
    private String country;

    /** Город доставки. Используется для определения региона и расчёта стоимости/сроков доставки. Не может быть пустым. */
    @Column(nullable = false)
    private String city;

    /** Улица и номер дома/квартиры. Конкретный адрес для доставки заказа. Не может быть пустым. */
    @Column(nullable = false)
    private String street;

    /** Флаг основного адреса доставки. {@code true} — данный адрес используется по умолчанию при оформлении заказа, если пользователь не указал другой. */
    @Column(name = "is_default")
    private boolean isDefault;

    /** Пользовательское название адреса (например, "Дом", "Работа", "Коттедж"). Позволяет удобно идентифицировать адрес в интерфейсе. */
    private String label;

    /** Ссылка на пользователя-владельца данного адреса. Определяет, кому принадлежит адрес доставки. Не может быть null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
