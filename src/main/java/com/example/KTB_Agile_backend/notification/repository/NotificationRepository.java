package com.example.KTB_Agile_backend.notification.repository;

import com.example.KTB_Agile_backend.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@Query("""
			select notification
			from Notification notification
			where notification.recipientUserId = :recipientUserId
			order by notification.createdAt desc, notification.id desc
			""")
	List<Notification> findRecentByRecipient(
			@Param("recipientUserId") Long recipientUserId,
			Pageable pageable
	);

	@Query("""
			select notification
			from Notification notification
			where notification.recipientUserId = :recipientUserId
			  and (notification.createdAt < :cursorCreatedAt
			       or (notification.createdAt = :cursorCreatedAt and notification.id < :cursorId))
			order by notification.createdAt desc, notification.id desc
			""")
	List<Notification> findRecentByRecipientAfter(
			@Param("recipientUserId") Long recipientUserId,
			@Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
			@Param("cursorId") Long cursorId,
			Pageable pageable
	);
}
