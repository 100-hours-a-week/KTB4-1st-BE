package com.example.KTB_Agile_backend.ai.chatbot.service;

import com.example.KTB_Agile_backend.ai.chatbot.dto.ai.ChatbotAiRequest;
import com.example.KTB_Agile_backend.ai.chatbot.entity.ChatbotMessage;
import com.example.KTB_Agile_backend.ai.chatbot.repository.ChatbotMessageRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotHistoryService {
	private static final int MAX_HISTORY = 10;

	private final ChatbotMessageRepository chatbotMessageRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<ChatbotAiRequest.HistoryMessage> findRecentHistory(Long userId) {
		List<ChatbotMessage> messages = new ArrayList<>(
				chatbotMessageRepository.findAllByUser_IdOrderByIdDesc(userId, PageRequest.of(0, MAX_HISTORY))
		);
		Collections.reverse(messages);
		return messages.stream()
				.map(message -> new ChatbotAiRequest.HistoryMessage(
						message.getRole().aiValue(),
						message.getContent()
				))
				.toList();
	}

	@Transactional
	public void saveExchange(Long userId, String userMessage, String assistantMessage) {
		User user = userRepository.getReferenceById(userId);
		chatbotMessageRepository.saveAll(List.of(
				new ChatbotMessage(user, ChatbotMessage.Role.USER, userMessage),
				new ChatbotMessage(user, ChatbotMessage.Role.ASSISTANT, assistantMessage)
		));
		List<ChatbotMessage> latestMessages = chatbotMessageRepository.findAllByUser_IdOrderByIdDesc(
				userId,
				PageRequest.of(0, MAX_HISTORY)
		);
		if (latestMessages.size() == MAX_HISTORY) {
			Long oldestId = latestMessages.get(latestMessages.size() - 1).getId();
			chatbotMessageRepository.deleteOlderThan(userId, oldestId);
		}
	}
}
