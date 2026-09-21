package com.kov.techuserservice.service;

import com.kov.techuserservice.dto.address.AddressDTO;

import java.util.List;

public interface AddressService {

    List<AddressDTO> getAllAddresses(Long userId);

    AddressDTO getDefaultAddress(Long userId);

    AddressDTO addAddress(Long userId, AddressDTO dto);

    AddressDTO updateAddress(Long userId, Long addressId, AddressDTO dto);

    void deleteAddress(Long userId, Long addressId);

    AddressDTO setDefaultAddress(Long userId, Long addressId);
}
