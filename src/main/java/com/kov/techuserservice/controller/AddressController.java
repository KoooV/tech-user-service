package com.kov.techuserservice.controller;

import com.kov.techuserservice.dto.address.AddressDTO;
import com.kov.techuserservice.entity.Address;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.AddressRepository;
import com.kov.techuserservice.entity.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getAllAddresses(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        List<Address> addresses = addressRepository.findByUserIdOrderByIsDefaultDesc(userId);
        return ResponseEntity.ok(addresses.stream().map(this::toDTO).toList());
    }

    @GetMapping("/default")
    public ResponseEntity<AddressDTO> getDefaultAddress(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userId).stream()
                .filter(Address::isDefault)
                .findFirst()
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<AddressDTO> addAddress(@PathVariable Long userId, @Valid @RequestBody AddressDTO dto) {
        if (!userRepository.existsById(userId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Address address = new Address();
        address.setCountry(dto.getCountry());
        address.setCity(dto.getCity());
        address.setStreet(dto.getStreet());
        address.setDefault(dto.isDefault());
        address.setLabel(dto.getLabel());
        User user = userRepository.getReferenceById(userId);
        address.setUser(user);
        Address saved = addressRepository.save(address);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(toDTO(saved));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long userId, @PathVariable Long addressId, @Valid @RequestBody AddressDTO dto) {
        return addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(userId))
                .map(address -> {
                    address.setCountry(dto.getCountry());
                    address.setCity(dto.getCity());
                    address.setStreet(dto.getStreet());
                    address.setDefault(dto.isDefault());
                    address.setLabel(dto.getLabel());
                    Address saved = addressRepository.save(address);
                    return ResponseEntity.ok(toDTO(saved));
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        return addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(userId))
                .map(address -> {
                    addressRepository.delete(address);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElse(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<AddressDTO> setDefaultAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        return addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(userId))
                .map(address -> {
                    addressRepository.findByUserIdOrderByIsDefaultDesc(userId).forEach(a -> a.setDefault(false));
                    address.setDefault(true);
                    Address saved = addressRepository.save(address);
                    return ResponseEntity.ok(toDTO(saved));
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private AddressDTO toDTO(Address address) {
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setCountry(address.getCountry());
        dto.setCity(address.getCity());
        dto.setStreet(address.getStreet());
        dto.setDefault(address.isDefault());
        dto.setLabel(address.getLabel());
        return dto;
    }
}