package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatRoomRepository;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMemberRepository chatMemberRepository;

	@Transactional
	public void leave(Long chatRoomId, Long userId) {
		if (!chatRoomRepository.existsById(chatRoomId)) {
			throw new ApiException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다.");
		}

		ChatMember member = chatMemberRepository.findActiveMember(chatRoomId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "참여 중인 채팅방만 나갈 수 있습니다."));
		member.leave(LocalDateTime.now());
	}
}
