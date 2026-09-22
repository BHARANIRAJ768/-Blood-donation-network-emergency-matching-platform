package com.lifelink.controller;

import com.lifelink.dto.BloodRequestCreateRequest;
import com.lifelink.dto.BloodRequestResponse;
import com.lifelink.dto.MessageResponse;
import com.lifelink.security.CurrentUserService;
import com.lifelink.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;
    private final CurrentUserService currentUserService;

    public RequestController(RequestService requestService, CurrentUserService currentUserService) {
        this.requestService = requestService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<BloodRequestResponse> create(@Valid @RequestBody BloodRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.create(currentUserService.getCurrentUserId(), request));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BloodRequestResponse>> listMine() {
        return ResponseEntity.ok(requestService.listForPatient(currentUserService.getCurrentUserId()));
    }

    @GetMapping("/donor")
    public ResponseEntity<List<BloodRequestResponse>> listMineAsDonor() {
        return ResponseEntity.ok(requestService.listForDonor(currentUserService.getCurrentUserId()));
    }

    @GetMapping("/available")
    public ResponseEntity<List<BloodRequestResponse>> listAvailableForDonor() {
        return ResponseEntity.ok(requestService.listAvailableForDonor(currentUserService.getCurrentUserId()));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<BloodRequestResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.accept(id, currentUserService.getCurrentUserId()));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<MessageResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.reject(id, currentUserService.getCurrentUserId()));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<BloodRequestResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.complete(id, currentUserService.getCurrentUserId()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BloodRequestResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.cancel(id, currentUserService.getCurrentUserId()));
    }

    @GetMapping("/email-accept")
    public ResponseEntity<?> emailAccept(@RequestParam String token) {
        try {
            BloodRequestResponse response = requestService.handleEmailAccept(token);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
        }
    }

    @GetMapping("/email-decline")
    public ResponseEntity<?> emailDecline(@RequestParam String token) {
        try {
            MessageResponse response = requestService.handleEmailDecline(token);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
        }
    }
}
