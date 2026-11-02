package com.example.KTB_Agile_backend.notification.service;

import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.notification.dto.NotificationMessage;
import com.example.KTB_Agile_backend.notification.entity.Notification;
import com.example.KTB_Agile_backend.notification.entity.NotificationEventType;
import com.example.KTB_Agile_backend.notification.messaging.NotificationMessagePublisher;
import com.example.KTB_Agile_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.MANDATORY)
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final ExchangeRequestRepository exchangeRequestRepository;
	private final NotificationMessagePublisher notificationMessagePublisher;

	public void requestCreated(ExchangeRequest request) {
		persist(
				request.getItem().getUser().getId(),
				NotificationEventType.REQUEST_CREATED,
				request,
				request.getItem(),
				request.getRequester().getNickname() + "님이 교환을 요청했습니다."
		);
	}

	public void conditionsChanged(ExchangeRequest request) {
		persist(
				request.getItem().getUser().getId(),
				NotificationEventType.CONDITIONS_CHANGED,
				request,
				request.getItem(),
				request.getRequester().getNickname() + "님이 교환 조건을 수정했습니다."
		);
	}

	public void requestCanceled(ExchangeRequest request) {
		persist(
				request.getItem().getUser().getId(),
				NotificationEventType.REQUEST_CANCELED,
				request,
				request.getItem(),
				request.getRequester().getNickname() + "님이 교환 요청을 취소했습니다."
		);
	}

	public void requestCompleted(ExchangeRequest request) {
		persist(
				request.getRequester().getId(),
				NotificationEventType.REQUEST_COMPLETED,
				request,
				request.getItem(),
				request.getItem().getUser().getNickname() + "님과의 교환이 완료되었습니다."
		);
	}

	public void requestRejected(ExchangeRequest request) {
		persist(
				request.getRequester().getId(),
				NotificationEventType.REQUEST_REJECTED,
				request,
				request.getItem(),
				request.getItem().getUser().getNickname() + "님이 교환 요청을 거절했습니다."
		);
	}

	public void itemUnavailable(Item item, Long excludedExchangeRequestId) {
		List<ExchangeRequest> pendingRequests = exchangeRequestRepository.findAllByItem_IdAndRequestedStatus(
				item.getId(), ExchangeRequestStatus.PENDING);
		if (pendingRequests.isEmpty()) {
			return;
		}

		String eventIdentity = UUID.randomUUID().toString();
		String text = item.getUser().getNickname() + "님과의 교환 요청 대상 물품이 거래 불가 상태로 변경되었습니다.";
		for (ExchangeRequest request : pendingRequests) {
			if (request.getId().equals(excludedExchangeRequestId)) {
				continue;
			}
			persist(
					request.getRequester().getId(),
					NotificationEventType.ITEM_UNAVAILABLE,
					request,
					item,
					eventIdentity,
					text
			);
		}
	}

	private void persist(
			Long recipientUserId,
			NotificationEventType eventType,
			ExchangeRequest request,
			Item item,
			String text
	) {
		persist(recipientUserId, eventType, request, item, UUID.randomUUID().toString(), text);
	}

	private void persist(
			Long recipientUserId,
			NotificationEventType eventType,
			ExchangeRequest request,
			Item item,
			String eventIdentity,
			String text
	) {
		Notification notification = notificationRepository.save(new Notification(
				recipientUserId,
				eventType,
				eventIdentity,
				request.getId(),
				item.getId(),
				text
		));
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					notificationMessagePublisher.publish(recipientUserId, new NotificationMessage(
							notification.getId(), notification.getCreatedAt(), notification.getText()));
				} catch (RuntimeException exception) {
					log.warn("Could not publish notification {}", notification.getId(), exception);
				}
			}
		});
	}
}
