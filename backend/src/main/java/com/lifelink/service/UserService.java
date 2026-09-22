package com.lifelink.service;

import com.lifelink.dto.ChangePasswordRequest;
import com.lifelink.dto.UserProfileResponse;
import com.lifelink.dto.UserProfileUpdateRequest;
import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;
import com.lifelink.exception.BadRequestException;
import com.lifelink.exception.DuplicateResourceException;
import com.lifelink.exception.ResourceNotFoundException;
import com.lifelink.repository.UserRepository;
import com.lifelink.util.Haversine;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final int MAX_PHOTO_LENGTH = 5_500_000;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = findUser(userId);

        user.setName(request.name().trim());

        String phone = request.phone().trim();
        if (!phone.equals(user.getPhone())) {
            if (userRepository.existsByPhone(phone)) {
                throw new DuplicateResourceException("This phone number is already registered");
            }
            user.setPhone(phone);
        }

        if (request.bloodGroup() != null && !request.bloodGroup().isBlank()) {
            user.setBloodGroup(BloodGroup.from(request.bloodGroup()));
        }

        user.setDistrict(trimToNull(request.district()));
        user.setAvailable(request.available());

        validateCoordinates(request.latitude(), request.longitude());
        user.setLatitude(request.latitude());
        user.setLongitude(request.longitude());

        if (request.photo() != null) {
            validatePhoto(request.photo());
            user.setPhoto(request.photo());
        }

        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateAvailability(Long userId, boolean available) {
        User user = findUser(userId);
        user.setAvailable(available);
        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updatePhoto(Long userId, String photoData) {
        User user = findUser(userId);
        validatePhoto(photoData);
        user.setPhoto(photoData);
        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return;
        }
        if (!Haversine.isValidLatitude(latitude) || !Haversine.isValidLongitude(longitude)) {
            throw new BadRequestException("Location coordinates are out of range");
        }
    }

    private static void validatePhoto(String photo) {
        if (photo.length() > MAX_PHOTO_LENGTH) {
            throw new BadRequestException("Photo is too large. Maximum size is 5 MB");
        }
        String lower = photo.toLowerCase();
        if (!lower.startsWith("data:image/")) {
            throw new BadRequestException("Photo must be a valid image");
        }
        boolean isImage = lower.contains("data:image/png") || lower.contains("data:image/jpeg")
                || lower.contains("data:image/jpg") || lower.contains("data:image/webp")
                || lower.contains("data:image/gif");
        if (!isImage) {
            throw new BadRequestException("Unsupported image format");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
