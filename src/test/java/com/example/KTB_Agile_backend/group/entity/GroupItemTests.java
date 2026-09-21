package com.example.KTB_Agile_backend.group.entity;

import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupItemTests {

	@Test
	void softDeletesGroupItem() {
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		Item item = new Item(new User("사용자"), "물품", "설명");
		GroupItem groupItem = new GroupItem(group, item);

		assertThat(groupItem.isActive()).isTrue();

		groupItem.delete();

		assertThat(groupItem.isActive()).isFalse();
		assertThat(groupItem.getDeletedAt()).isNotNull();
	}

	@Test
	void rejectsNullItem() {
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");

		assertThrows(NullPointerException.class, () -> new GroupItem(group, null));
	}
}
