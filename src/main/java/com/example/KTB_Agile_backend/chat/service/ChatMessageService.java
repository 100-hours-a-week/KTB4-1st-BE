package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.entity.ChatRoomStatus;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatMessageRepository;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private static final int MAX_CONTENT_LENGTH = 2000;

	private final ChatMemberRepository chatMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final GroupMemberRepository groupMemberRepository;

	@Transactional
	public ChatMessageResponse send(Long chatRoomId, Long senderId, String content) {
		if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
			throw new IllegalArgumentException("메시지는 1~2000자여야 합니다.");
		}

		ChatMember member = chatMemberRepository
				.findActiveMember(chatRoomId, senderId)
				.orElseThrow(() -> new AccessDeniedException("채팅방 멤버만 메시지를 보낼 수 있습니다."));
		if (member.getChatRoom().isDeleted()
				|| member.getChatRoom().getChatRoomStatus() != ChatRoomStatus.OPEN) {
			throw new AccessDeniedException("현재 채팅방에서 메시지를 보낼 수 없습니다.");
		}

		ChatMessage message = chatMessageRepository.save(
				new ChatMessage(member.getChatRoom(), member.getUser(), content));
		member.getChatRoom().updateLastMessageAt(message.getCreatedAt());
		Group group = member.getChatRoom().getExchangeRequest().getGroup();
		GroupMemberStatus groupMemberStatus = group == null ? null
				: groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), senderId)
						.map(groupMember -> groupMember.getStatus())
						.orElse(null);

		return toResponse(message, groupMemberStatus);
	}

	@Transactional
	public void markReadThrough(Long chatRoomId, Long userId, Long messageId) {
		ChatMember member = chatMemberRepository.findActiveMember(chatRoomId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
		member.markReadThrough(messageId);
	}

	private static ChatMessageResponse toResponse(ChatMessage message, GroupMemberStatus groupMemberStatus) {
		return new ChatMessageResponse(
				message.getId(), message.getChatRoom().getId(), message.getUser().getId(),
				message.getContent(), message.getMessageType(), message.getCreatedAt(),
				message.getUser().getUserStatus(), groupMemberStatus
		);
	}
}
