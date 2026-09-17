package com.example.KTB_Agile_backend.group.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class GroupTests {

	@Test
	void normalizesValuesAndDefaultsContent() {
		Group group = Group.create(
				"  우리 그룹  ",
				"  서울시 중구 세종대로 1  ",
				new BigDecimal("126.978000"),
				new BigDecimal("37.566500"),
				null
		);

		assertThat(group.getGroupName()).isEqualTo("우리 그룹");
		assertThat(group.getRoadAddress()).isEqualTo("서울시 중구 세종대로 1");
		assertThat(group.getGroupContent()).isEmpty();
	}

	@Test
	void rejectsInvalidCoordinates() {
		assertThatIllegalArgumentException().isThrownBy(() -> Group.create(
				"그룹",
				"주소",
				new BigDecimal("181"),
				BigDecimal.ZERO,
				null
		));
	}

	@Test
	void softDeletesGroup() {
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, null);

		group.delete();

		assertThat(group.getDeletedAt()).isNotNull();
	}
}
