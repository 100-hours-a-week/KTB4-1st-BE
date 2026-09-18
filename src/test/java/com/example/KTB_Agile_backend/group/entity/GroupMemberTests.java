package com.example.KTB_Agile_backend.group.entity;

import com.example.KTB_Agile_backend.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupMemberTests {

	@Test
	void joinsLeavesAndRejoinsSameMember() {
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		GroupMember member = new GroupMember(
				group,
				new User("사용자"),
				LocalDateTime.of(2026, 9, 17, 10, 0)
		);

		assertThat(member.isActive()).isTrue();

		LocalDateTime leftAt = LocalDateTime.of(2026, 9, 17, 11, 0);
		member.leave(leftAt);
		assertThat(member.isActive()).isFalse();
		assertThat(member.getLeftAt()).isEqualTo(leftAt);

		LocalDateTime rejoinedAt = LocalDateTime.of(2026, 9, 17, 12, 0);
		member.join(rejoinedAt);
		assertThat(member.isActive()).isTrue();
		assertThat(member.getLeftAt()).isNull();
		assertThat(member.getLocationVerifiedAt()).isEqualTo(rejoinedAt);
	}

	@Test
	void requiresVerificationAndLeaveTimes() {
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		User user = new User("사용자");

		assertThrows(NullPointerException.class, () -> new GroupMember(group, user, null));

		GroupMember member = new GroupMember(group, user, LocalDateTime.now());
		assertThrows(NullPointerException.class, () -> member.leave(null));
	}
}
