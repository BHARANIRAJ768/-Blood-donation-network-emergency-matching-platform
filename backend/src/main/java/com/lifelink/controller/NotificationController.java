package com.lifelink.controller;

import com.lifelink.dto.BloodRequestResponse;
import com.lifelink.dto.CountResponse;
import com.lifelink.dto.MessageResponse;
import com.lifelink.dto.NotificationResponse;
import com.lifelink.security.CurrentUserService;
import com.lifelink.service.NotificationService;
import com.lifelink.service.RequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestService requestService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService,
                                  RequestService requestService,
                                  CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.requestService = requestService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        Long userId = currentUserService.getCurrentUserId();
        List<NotificationResponse> result = unreadOnly
                ? notificationService.getUnread(userId, Math.min(limit, 50))
                : notificationService.getRecent(userId, Math.min(limit, 50));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<CountResponse> unreadCount() {
        return ResponseEntity.ok(new CountResponse(notificationService.countUnread(currentUserService.getCurrentUserId())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(id, currentUserService.getCurrentUserId()));
    }

    @PutMapping("/read-all")
    public ResponseEntity<CountResponse> markAllRead() {
        return ResponseEntity.ok(new CountResponse(notificationService.markAllRead(currentUserService.getCurrentUserId())));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<BloodRequestResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.acceptFromNotification(id, currentUserService.getCurrentUserId()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<MessageResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.rejectFromNotification(id, currentUserService.getCurrentUserId()));
    }
}
