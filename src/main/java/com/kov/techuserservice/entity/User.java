package com.kov.techuserservice.entity;

import com.kov.techuserservice.dto.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Сущность пользователя — основная модель домена сервиса пользователей.
 * Реализует {@link UserDetails} для интеграции со Spring Security (JWT-аутентификация).
 * Каждый пользователь имеет роли и адреса доставки.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /** Уникальный идентификатор пользователя (первичный ключ). Автоматически генерируется при создании записи. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Имя пользователя. Отображаемое имя, используется в интерфейсе и уведомлениях. Не может быть пустым. */
    @Column(nullable = false)
    private String firstName;

    /** Фамилия пользователя. Используется вместе с firstName для формирования полного имени. Не может быть пустым. */
    @Column(nullable = false)
    private String lastName;

    /** Электронная почта — уникальный логин для входа и идентификации в системе. Является уникальным индексом, обеспечивает вход и получение уведомлений. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Пароль пользователя в хешированном виде (BCrypt). Хранится в зашифрованном виде, никогда не передаётся клиенту. */
    @Column(nullable = false)
    private String password;

    /** Телефонный номер в международном формате. Используется для SMS-уведомлений и дополнительной верификации. */
    @Column(nullable = false)
    private String phone;

    /** Флаг активности аккаунта. {@code true} — аккаунт активен и доступен для входа, {@code false} — заблокирован или деактивирован администратором. */
    private boolean active;

    /** Дата и время создания записи пользователя. Устанавливается автоматически при первом сохранении и не изменяется thereafter. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления профиля. Автоматически пересчитывается при каждом изменении данных пользователя. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Множество ролей пользователя. Определяет уровень доступа и набор разрешений в системе. Загружается немедленно (EAGER) при загрузке пользователя для обеспечения безопасности Spring Security. */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /** Список адресов доставки пользователя. Один из адресов может быть помечен как основной (isDefault). При удалении пользователя все адреса каскадно удаляются. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isDefault DESC")
    private List<Address> addresses = new ArrayList<>();

    /** Колбэк, вызываемый перед сохранением сущности в БД. Устанавливает дату создания и обновления. */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /** Колбэк, вызываемый перед обновлением сущности в БД. Обновляет дату изменения. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Возвращает совокупность разрешений (authorities) пользователя, полученных из всех назначенных ролей. Используется Spring Security для проверки прав доступа. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(permission -> new SimpleGrantedAuthority(permission.getName()))
            .toList();
    }

    /** Возвращает email пользователя в качестве имени пользователя для Spring Security (логин). */
    @Override
    public String getUsername() {
        return email;
    }

    /** Указывает, не истёк ли срок действия учётной записи. Всегда {@code true} — ограничение срока не используется. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Указывает, заблокирована ли учётная запись. Всегда {@code true} — блокировка реализуется через поле {@code active}. */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** Указывает, не истёк ли срок действия учётных данных (пароля). Всегда {@code true} — ограничение срока пароля не используется. */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Указывает, активен ли аккаунт для входа. Возвращает значение поля {@code active}. */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
