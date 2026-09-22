package com.lifelink.service;

import com.lifelink.dto.NotificationResponse;
import com.lifelink.entity.Notification;
import com.lifelink.entity.User;
import com.lifelink.exception.ResourceNotFoundException;
import com.lifelink.repository.NotificationRepository;
import com.lifelink.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Notification create(Long receiverId, String title, String message, Long requestId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification receiver not found"));
        Notification notification = new Notification();
        notification.setReceiver(receiver);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRequestId(requestId);
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void createToMany(List<Long> receiverIds, String title, String message, Long requestId) {
        for (Long receiverId : receiverIds) {
            create(receiverId, title, message, requestId);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecent(Long receiverId, int limit) {
        return notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(receiverId, PageRequest.of(0, limit))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread(Long receiverId, int limit) {
        return notificationRepository
                .findByReceiverIdAndReadFalseOrderByCreatedAtDesc(receiverId, PageRequest.of(0, limit))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(Long receiverId) {
        return notificationRepository.countByReceiverIdAndReadFalse(receiverId);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long receiverId) {
        Notification notification = findForUser(notificationId, receiverId);
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public long markAllRead(Long receiverId) {
        return notificationRepository.markAllRead(receiverId);
    }

    public Notification findForUser(Long notificationId, Long receiverId) {
        return notificationRepository.findByIdAndReceiverId(notificationId, receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }
}
