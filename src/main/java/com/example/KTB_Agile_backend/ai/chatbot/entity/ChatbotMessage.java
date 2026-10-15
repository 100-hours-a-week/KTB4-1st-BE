package com.example.KTB_Agile_backend.ai.chatbot.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chatbot_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatbotMessage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chatbot_message_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "message_role", nullable = false, length = 20)
	private Role role;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	public ChatbotMessage(User user, Role role, String content) {
		this.user = user;
		this.role = role;
		this.content = content;
	}

	public enum Role {
		USER("user"),
		ASSISTANT("assistant");

		private final String aiValue;

		Role(String aiValue) {
			this.aiValue = aiValue;
		}

		public String aiValue() {
			return aiValue;
		}
	}
}
