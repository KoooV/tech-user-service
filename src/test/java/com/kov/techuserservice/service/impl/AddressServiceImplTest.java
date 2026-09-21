package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.dto.address.AddressDTO;
import com.kov.techuserservice.entity.Address;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.AddressRepository;
import com.kov.techuserservice.entity.repository.UserRepository;
import com.kov.techuserservice.exception.AddressNotFoundException;
import com.kov.techuserservice.exception.UserNotFoundException;
import com.kov.techuserservice.mapper.AddressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User testUser;
    private Address defaultAddress;
    private Address plainAddress;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        defaultAddress = Address.builder()
                .id(10L).country("RU").city("Moscow").street("Tverskaya 1")
                .isDefault(true).label("Home").user(testUser)
                .build();

        plainAddress = Address.builder()
                .id(11L).country("RU").city("Moscow").street("Arbat 5")
                .isDefault(false).label("Work").user(testUser)
                .build();
    }

    @Test
    void getAllAddresses_ShouldReturnMappedList() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findByUserIdOrderByIsDefaultDesc(1L))
                .thenReturn(List.of(defaultAddress, plainAddress));
        when(addressMapper.toDTO(defaultAddress)).thenReturn(AddressDTO.builder().id(10L).build());
        when(addressMapper.toDTO(plainAddress)).thenReturn(AddressDTO.builder().id(11L).build());

        List<AddressDTO> result = addressService.getAllAddresses(1L);

        assertEquals(List.of(10L, 11L), result.stream().map(AddressDTO::getId).toList());
    }

    @Test
    void getAllAddresses_MissingUser_ShouldThrow() {
        when(userRepository.existsById(42L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> addressService.getAllAddresses(42L));
        verify(addressRepository, never()).findByUserIdOrderByIsDefaultDesc(any());
    }

    @Test
    void getDefaultAddress_ShouldPickFlaggedAddress() {
        when(userRepository.existsById(1L)).thenReturn(true);
        // Порядок намеренно «неправильный»: фильтр должен найти default, а не просто взять первый.
        when(addressRepository.findByUserIdOrderByIsDefaultDesc(1L))
                .thenReturn(List.of(plainAddress, defaultAddress));
        AddressDTO dto = AddressDTO.builder().id(10L).isDefault(true).build();
        when(addressMapper.toDTO(defaultAddress)).thenReturn(dto);

        assertEquals(dto, addressService.getDefaultAddress(1L));
    }

    @Test
    void getDefaultAddress_NoDefault_ShouldThrow() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findByUserIdOrderByIsDefaultDesc(1L))
                .thenReturn(List.of(plainAddress));

        assertThrows(AddressNotFoundException.class, () -> addressService.getDefaultAddress(1L));
    }

    @Test
    void addAddress_NonDefault_ShouldLinkUserWithoutTouchingOthers() {
        AddressDTO dto = AddressDTO.builder().country("RU").city("Moscow").street("Arbat 5").build();
        Address entity = Address.builder().country("RU").city("Moscow").street("Arbat 5").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressMapper.toEntity(dto)).thenReturn(entity);
        when(addressRepository.save(entity)).thenReturn(plainAddress);
        when(addressMapper.toDTO(plainAddress)).thenReturn(AddressDTO.builder().id(11L).build());

        AddressDTO result = addressService.addAddress(1L, dto);

        assertEquals(11L, result.getId());
        assertSame(testUser, entity.getUser());
        verify(addressRepository, never()).saveAll(any());
    }

    @Test
    void addAddress_Default_ShouldUnsetPreviousDefaults() {
        AddressDTO dto = AddressDTO.builder()
                .country("RU").city("SPb").street("Nevsky 1").isDefault(true).build();
        Address entity = Address.builder()
                .country("RU").city("SPb").street("Nevsky 1").isDefault(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressMapper.toEntity(dto)).thenReturn(entity);
        when(addressRepository.findByUserIdOrderByIsDefaultDesc(1L))
                .thenReturn(List.of(defaultAddress));
        when(addressRepository.save(entity)).thenReturn(entity);
        when(addressMapper.toDTO(entity)).thenReturn(dto);

        addressService.addAddress(1L, dto);

        assertFalse(defaultAddress.isDefault());
        verify(addressRepository, times(1)).saveAll(any());
    }

    @Test
    void addAddress_MissingUser_ShouldThrow() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> addressService.addAddress(42L, AddressDTO.builder().build()));
        verify(addressRepository, never()).save(any());
    }

    @Test
    void updateAddress_ShouldKeepIdAndOwner() {
        AddressDTO dto = AddressDTO.builder().country("RU").city("Kazan").street("Baumana 1").build();
        Address mapped = Address.builder().country("RU").city("Kazan").street("Baumana 1").build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(11L)).thenReturn(Optional.of(plainAddress));
        when(addressMapper.toEntity(dto)).thenReturn(mapped);
        when(addressRepository.save(any(Address.class))).thenReturn(mapped);
        when(addressMapper.toDTO(mapped)).thenReturn(AddressDTO.builder().id(11L).city("Kazan").build());

        AddressDTO result = addressService.updateAddress(1L, 11L, dto);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertEquals(11L, captor.getValue().getId());
        assertSame(testUser, captor.getValue().getUser());
        assertEquals("Kazan", result.getCity());
        // Повышения до default не было — сброс предыдущих не требуется.
        verify(addressRepository, never()).saveAll(any());
    }

    @Test
    void updateAddress_PromotionToDefault_ShouldUnsetPreviousDefaults() {
        AddressDTO dto = AddressDTO.builder().country("RU").city("Moscow").street("Arbat 5").isDefault(true).build();
        Address mapped = Address.builder().country("RU").city("Moscow").street("Arbat 5").isDefault(true).build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(11L)).thenReturn(Optional.of(plainAddress));
        when(addressMapper.toEntity(dto)).thenReturn(mapped);
        when(addressRepository.findByUserIdOrderByIsDefaultDesc(1L))
                .thenReturn(List.of(defaultAddress, plainAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));
        when(addressMapper.toDTO(any(Address.class))).thenReturn(dto);

        addressService.updateAddress(1L, 11L, dto);

        verify(addressRepository, times(1)).saveAll(any());
    }

    @Test
    void updateAddress_ForeignAddress_ShouldThrow() {
        User stranger = new User();
        stranger.setId(2L);
        Address foreign = Address.builder().id(99L).country("US").city("NYC").street("5th Ave")
                .user(stranger).build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(99L)).thenReturn(Optional.of(foreign));

        assertThrows(AddressNotFoundException.class,
                () -> addressService.updateAddress(1L, 99L, AddressDTO.builder().build()));
        verify(addressRepository, never()).save(any());
    }

    @Test
    void deleteAddress_ShouldDeleteOwnAddress() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(11L)).thenReturn(Optional.of(plainAddress));

        addressService.deleteAddress(1L, 11L);

        verify(addressRepository, times(1)).delete(plainAddress);
    }

    @Test
    void deleteAddress_ForeignAddress_ShouldThrowWithoutDeleting() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class, () -> addressService.deleteAddress(1L, 42L));
        verify(addressRepository, never()).delete(any());
    }

    @Test
    void setDefaultAddress_ShouldUnsetOthersAndFlagTarget() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(11L)).thenReturn(Optional.of(plainAddress));
        when(addressRepository.findByUserIdOrderByIsDefaultDesc(1L))
                .thenReturn(List.of(defaultAddress, plainAddress));
        when(addressRepository.save(plainAddress)).thenReturn(plainAddress);
        when(addressMapper.toDTO(plainAddress)).thenReturn(AddressDTO.builder().id(11L).isDefault(true).build());

        AddressDTO result = addressService.setDefaultAddress(1L, 11L);

        assertFalse(defaultAddress.isDefault());
        assertTrue(plainAddress.isDefault());
        assertTrue(result.isDefault());
        verify(addressRepository, times(1)).saveAll(any());
        verify(addressRepository, times(1)).save(plainAddress);
    }
}
