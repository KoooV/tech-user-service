package com.kov.techuserservice.repository;

import com.kov.techuserservice.TestcontainersConfiguration;
import com.kov.techuserservice.entity.Address;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.AddressRepository;
import com.kov.techuserservice.entity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Репозиторные тесты адресов: выборка с сортировкой (default first),
 * каскадное удаление вместе с пользователем (JPA + FK {@code ON DELETE CASCADE} в V1).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUserWithAddresses(String email) {
        User user = new User();
        user.setFirstName("Addr");
        user.setLastName("Owner");
        user.setEmail(email);
        user.setPassword("$2a$10$hashed");
        user.setPhone("+1234567890");
        user.setActive(true);

        Address secondary = Address.builder()
                .country("KZ").city("Almaty").street("Abaya 1")
                .isDefault(false).label("Work").user(user).build();
        Address primary = Address.builder()
                .country("KZ").city("Astana").street("Mangilik 1")
                .isDefault(true).label("Home").user(user).build();
        // Порядок вставки обратный — проверка сортировки default-first при чтении.
        user.getAddresses().add(secondary);
        user.getAddresses().add(primary);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void findByUserIdOrderByIsDefaultDesc_ShouldReturnDefaultFirst() {
        User user = savedUserWithAddresses("addr-order@example.com");

        var addresses = addressRepository.findByUserIdOrderByIsDefaultDesc(user.getId());

        assertThat(addresses).hasSize(2);
        assertThat(addresses.get(0).isDefault()).isTrue();
        assertThat(addresses.get(0).getCity()).isEqualTo("Astana");
    }

    @Test
    void deleteUser_ShouldCascadeAddresses() {
        User user = savedUserWithAddresses("addr-cascade@example.com");
        Long userId = user.getId();
        assertThat(addressRepository.findByUserIdOrderByIsDefaultDesc(userId)).hasSize(2);

        userRepository.delete(user);
        userRepository.flush();

        assertThat(addressRepository.findByUserIdOrderByIsDefaultDesc(userId)).isEmpty();
    }
}
