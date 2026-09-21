package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.dto.address.AddressDTO;
import com.kov.techuserservice.entity.Address;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.AddressRepository;
import com.kov.techuserservice.entity.repository.UserRepository;
import com.kov.techuserservice.exception.AddressNotFoundException;
import com.kov.techuserservice.exception.UserNotFoundException;
import com.kov.techuserservice.mapper.AddressMapper;
import com.kov.techuserservice.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getAllAddresses(Long userId) {
        ensureUserExists(userId);
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userId).stream()
                .map(addressMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getDefaultAddress(Long userId) {
        ensureUserExists(userId);
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userId).stream()
                .filter(Address::isDefault)
                .findFirst()
                .map(addressMapper::toDTO)
                .orElseThrow(() -> new AddressNotFoundException("Default address not found for user id: " + userId));
    }

    @Override
    @Transactional
    public AddressDTO addAddress(Long userId, AddressDTO dto) {
        User user = findUserOrThrow(userId);
        Address address = addressMapper.toEntity(dto);
        address.setUser(user);
        if (address.isDefault()) {
            unsetPreviousDefaults(userId);
        }
        Address saved = addressRepository.save(address);
        log.info("Address {} added for user {}", saved.getId(), userId);
        return addressMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long userId, Long addressId, AddressDTO dto) {
        ensureUserExists(userId);
        Address address = findUserAddressOrThrow(userId, addressId);
        Address updated = addressMapper.toEntity(dto);
        updated.setId(address.getId());
        updated.setUser(address.getUser());
        if (updated.isDefault() && !address.isDefault()) {
            unsetPreviousDefaults(userId);
        }
        Address saved = addressRepository.save(updated);
        log.info("Address {} updated for user {}", addressId, userId);
        return addressMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        ensureUserExists(userId);
        Address address = findUserAddressOrThrow(userId, addressId);
        addressRepository.delete(address);
        log.info("Address {} deleted for user {}", addressId, userId);
    }

    @Override
    @Transactional
    public AddressDTO setDefaultAddress(Long userId, Long addressId) {
        ensureUserExists(userId);
        Address address = findUserAddressOrThrow(userId, addressId);
        unsetPreviousDefaults(userId);
        address.setDefault(true);
        Address saved = addressRepository.save(address);
        log.info("Address {} set as default for user {}", addressId, userId);
        return addressMapper.toDTO(saved);
    }

    private void unsetPreviousDefaults(Long userId) {
        List<Address> addresses = addressRepository.findByUserIdOrderByIsDefaultDesc(userId);
        addresses.forEach(a -> a.setDefault(false));
        addressRepository.saveAll(addresses);
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    private Address findUserAddressOrThrow(Long userId, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(a -> a.getUser() != null && a.getUser().getId().equals(userId))
                .orElseThrow(() -> new AddressNotFoundException(
                        "Address not found with id: " + addressId + " for user id: " + userId));
    }
}
