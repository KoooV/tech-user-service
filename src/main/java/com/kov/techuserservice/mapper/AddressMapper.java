package com.kov.techuserservice.mapper;

import com.kov.techuserservice.dto.address.AddressDTO;
import com.kov.techuserservice.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressMapper INSTANCE = Mappers.getMapper(AddressMapper.class);

    AddressDTO toDTO(Address address);

    Address toEntity(AddressDTO dto);
}