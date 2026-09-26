package com.example.KTB_Agile_backend.chat.entity;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMember extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_members_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chat_room_id", nullable = false)
	private ChatRoom chatRoom;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "member_role", nullable = false, length = 20)
	private ChatMemberRole memberRole;

	@Column(name = "left_at")
	private LocalDateTime leftAt;

	public ChatMember(ChatRoom chatRoom, User user, ChatMemberRole memberRole) {
		this.chatRoom = chatRoom;
		this.user = user;
		this.memberRole = memberRole;
	}
}
