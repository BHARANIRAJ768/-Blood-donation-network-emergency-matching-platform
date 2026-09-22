package com.lifelink.controller;

import com.lifelink.dto.AvailabilityRequest;
import com.lifelink.dto.ChangePasswordRequest;
import com.lifelink.dto.MessageResponse;
import com.lifelink.dto.ProfilePhotoRequest;
import com.lifelink.dto.UserProfileResponse;
import com.lifelink.dto.UserProfileUpdateRequest;
import com.lifelink.security.CurrentUserService;
import com.lifelink.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(userService.getProfile(currentUserService.getCurrentUserId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(currentUserService.getCurrentUserId(), request));
    }

    @PutMapping("/availability")
    public ResponseEntity<UserProfileResponse> updateAvailability(@Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(userService.updateAvailability(currentUserService.getCurrentUserId(), request.available()));
    }

    @PostMapping("/photo")
    public ResponseEntity<UserProfileResponse> updatePhoto(@Valid @RequestBody ProfilePhotoRequest request) {
        return ResponseEntity.ok(userService.updatePhoto(currentUserService.getCurrentUserId(), request.photo()));
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserService.getCurrentUserId(), request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }
}
