package com.kov.techuserservice.dto.address;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для представления адреса доставки пользователя.
 * Используется при создании и обновлении адресов через {@code AddressController}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    /** Уникальный идентификатор адреса в базе данных. */
    private Long id;

    /** Страна доставки. Определяет международные правила отправки и налоговую политику. */
    @NotBlank
    private String country;

    /** Город доставки. Используется для определения региона и стоимости доставки. */
    @NotBlank
    private String city;

    /** Улица и номер дома/квартиры. Конкретный адрес доставки заказа. */
    @NotBlank
    private String street;

    /** Флаг основного адреса доставки. {@code true} — адрес используется по умолчанию при оформлении заказа. */
    private boolean isDefault;

    /** Пользовательское название адреса (например, "Дом", "Работа", "Коттедж"). Удобство при выборе адреса. */
    private String label;
}
