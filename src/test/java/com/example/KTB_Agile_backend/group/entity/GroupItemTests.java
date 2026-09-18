package com.example.KTB_Agile_backend.group.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GroupItemTests {

	@Test
	void softDeletesGroupItem() {
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		GroupItem groupItem = new GroupItem(group, 1L);

		assertThat(groupItem.isActive()).isTrue();

		groupItem.delete();

		assertThat(groupItem.isActive()).isFalse();
		assertThat(groupItem.getDeletedAt()).isNotNull();
	}
}
