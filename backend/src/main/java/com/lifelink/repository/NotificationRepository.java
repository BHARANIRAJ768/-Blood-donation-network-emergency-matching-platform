package com.lifelink.repository;

import com.lifelink.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    List<Notification> findByReceiverIdAndReadFalseOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndReadFalse(Long receiverId);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    List<Notification> findByReceiverIdAndRequestId(Long receiverId, Long requestId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.receiver.id = :receiverId AND n.read = false")
    int markAllRead(@Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.requestId = :requestId AND n.read = false")
    int markAllReadForRequest(@Param("requestId") Long requestId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.requestId = :requestId AND n.receiver.id <> :exceptId AND n.read = false")
    int markAllReadForRequestExcept(@Param("requestId") Long requestId, @Param("exceptId") Long exceptId);
}
