package com.kov.techuserservice.controller;

import com.kov.techuserservice.dto.address.AddressDTO;
import com.kov.techuserservice.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // USER — только свои адреса (userId == свой id), MANAGER/ADMIN — любые.
    private static final String SELF_OR_STAFF =
            "hasAnyRole('ADMIN', 'MANAGER') or " +
            "(authentication.principal instanceof T(com.kov.techuserservice.entity.User) " +
            "and #userId == authentication.principal.id)";

    @GetMapping
    @PreAuthorize(SELF_OR_STAFF)
    public ResponseEntity<List<AddressDTO>> getAllAddresses(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getAllAddresses(userId));
    }

    @GetMapping("/default")
    @PreAuthorize(SELF_OR_STAFF)
    public ResponseEntity<AddressDTO> getDefaultAddress(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getDefaultAddress(userId));
    }

    @PostMapping
    @PreAuthorize(SELF_OR_STAFF)
    public ResponseEntity<AddressDTO> addAddress(@PathVariable Long userId, @Valid @RequestBody AddressDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.addAddress(userId, dto));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize(SELF_OR_STAFF)
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long userId, @PathVariable Long addressId, @Valid @RequestBody AddressDTO dto) {
        return ResponseEntity.ok(addressService.updateAddress(userId, addressId, dto));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize(SELF_OR_STAFF)
    public ResponseEntity<Void> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{addressId}/default")
    @PreAuthorize(SELF_OR_STAFF)
    public ResponseEntity<AddressDTO> setDefaultAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.setDefaultAddress(userId, addressId));
    }
}
