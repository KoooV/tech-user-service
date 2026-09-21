package com.kov.techuserservice.controller;

import com.kov.techuserservice.dto.address.AddressDTO;
import com.kov.techuserservice.service.AddressService;
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

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getAllAddresses(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getAllAddresses(userId));
    }

    @GetMapping("/default")
    public ResponseEntity<AddressDTO> getDefaultAddress(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getDefaultAddress(userId));
    }

    @PostMapping
    public ResponseEntity<AddressDTO> addAddress(@PathVariable Long userId, @Valid @RequestBody AddressDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.addAddress(userId, dto));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long userId, @PathVariable Long addressId, @Valid @RequestBody AddressDTO dto) {
        return ResponseEntity.ok(addressService.updateAddress(userId, addressId, dto));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<AddressDTO> setDefaultAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.setDefaultAddress(userId, addressId));
    }
}
