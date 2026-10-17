package com.aventra.controller;

import java.util.List;
import java.util.Map;

import com.aventra.dto.LoginRequest;
import com.aventra.dto.LoginResponse;
import com.aventra.dto.OtpRequest;
import com.aventra.dto.OtpVerifyRequest;
import com.aventra.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.aventra.entity.User;
import com.aventra.service.OtpService;
import com.aventra.service.UserService;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = {"http://localhost:5500", "https://aventra-ecommerce.vercel.app"})
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private OtpService otpService;

    // Step 1: registration se pehle email par OTP bhejo
    @PostMapping("/send-otp")
    public Map<String, String> sendOtp(@Valid @RequestBody OtpRequest request) {
        return otpService.sendOtp(request);
    }

    // Step 2: OTP verify karo
    @PostMapping("/verify-otp")
    public Map<String, String> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return otpService.verifyOtp(request);
    }

    // Step 3: OTP verified hone ke baad hi register successful hoga
    // (UserService.registerUser() andar hi ensureEmailVerified() check karta hai)
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Login User
    @PostMapping("/login")
    public LoginResponse loginUser(@RequestBody LoginRequest request) {
        String token = userService.loginUser(request);
        return new LoginResponse(token);
    }

    // Logged-in user ka profile
    @GetMapping("/profile")
    public User getProfile() {
        return userService.getProfile();
    }

    @PutMapping("/profile")
    public User updateProfile(@RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(request);
    }
}
