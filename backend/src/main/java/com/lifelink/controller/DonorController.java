package com.lifelink.controller;

import com.lifelink.dto.PageResponse;
import com.lifelink.dto.DonorCardResponse;
import com.lifelink.security.CurrentUserService;
import com.lifelink.service.DonorService;
import com.lifelink.util.QueryParsers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DonorController {

    private final DonorService donorService;
    private final CurrentUserService currentUserService;

    public DonorController(DonorService donorService, CurrentUserService currentUserService) {
        this.donorService = donorService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/donors")
    public ResponseEntity<PageResponse<DonorCardResponse>> listDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(donorService.search(
                currentUserService.getCurrentUserId(),
                QueryParsers.bloodGroup(bloodGroup),
                parseDouble(latitude),
                parseDouble(longitude),
                QueryParsers.page(page),
                QueryParsers.size(size, 12, 50)));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<DonorCardResponse>> searchDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(donorService.search(
                currentUserService.getCurrentUserId(),
                QueryParsers.bloodGroup(bloodGroup),
                parseDouble(latitude),
                parseDouble(longitude),
                QueryParsers.page(page),
                QueryParsers.size(size, 12, 50)));
    }

    private static Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
