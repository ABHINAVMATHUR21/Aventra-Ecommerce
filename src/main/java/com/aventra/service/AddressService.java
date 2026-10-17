package com.aventra.service;

import com.aventra.dto.AddressRequest;
import com.aventra.entity.Address;
import com.aventra.entity.User;
import com.aventra.repository.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;

    public AddressService(
            AddressRepository addressRepository,
            CurrentUserService currentUserService
    ) {
        this.addressRepository = addressRepository;
        this.currentUserService = currentUserService;
    }

    public Address addAddress(AddressRequest request) {

        User user = currentUserService.getCurrentUser();

        if (request.isDefaultAddress()) {
            clearDefaultAddress(user.getId());
        }

        Address address = mapToEntity(request, new Address());
        address.setUser(user);

        return addressRepository.save(address);
    }

    public List<Address> getUserAddresses() {

        User user = currentUserService.getCurrentUser();

        return addressRepository.findByUser_Id(user.getId());
    }

    public Address updateAddress(Long addressId, AddressRequest request) {

        User user = currentUserService.getCurrentUser();

        Address address = getOwnedAddress(addressId, user.getId());

        if (request.isDefaultAddress()) {
            clearDefaultAddress(user.getId());
        }

        mapToEntity(request, address);

        return addressRepository.save(address);
    }

    public void deleteAddress(Long addressId) {

        User user = currentUserService.getCurrentUser();
        Address address = getOwnedAddress(addressId, user.getId());

        addressRepository.delete(address);
    }

    public Address getOwnedAddress(Long addressId, Long userId) {

        return addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
    }

    private void clearDefaultAddress(Long userId) {

        addressRepository.findByUser_Id(userId).forEach(address -> {
            if (address.isDefaultAddress()) {
                address.setDefaultAddress(false);
                addressRepository.save(address);
            }
        });
    }

    private Address mapToEntity(AddressRequest request, Address address) {

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setDefaultAddress(request.isDefaultAddress());

        return address;
    }
}
