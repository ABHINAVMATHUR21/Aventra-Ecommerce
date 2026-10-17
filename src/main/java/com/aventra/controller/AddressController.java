package com.aventra.controller;

import com.aventra.dto.AddressRequest;
import com.aventra.entity.Address;
import com.aventra.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@CrossOrigin(origins = "*")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @PostMapping
    public Address addAddress(@Valid @RequestBody AddressRequest request) {
        return addressService.addAddress(request);
    }

    @GetMapping
    public List<Address> getUserAddresses() {
        return addressService.getUserAddresses();
    }

    @PutMapping("/{addressId}")
    public Address updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request
    ) {
        return addressService.updateAddress(addressId, request);
    }

    @DeleteMapping("/{addressId}")
    public String deleteAddress(@PathVariable Long addressId) {
        addressService.deleteAddress(addressId);
        return "Address deleted successfully";
    }
}