package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.item.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupItemRepository extends JpaRepository<GroupItem, Long> {

	@Query("""
			select groupItem.item
			from GroupItem groupItem
			where groupItem.group.id = :groupId
				and groupItem.deletedAt is null
				and groupItem.item.deletedAt is null
			order by groupItem.item.id desc
			""")
	List<Item> findActiveItemsByGroupId(
			@Param("groupId") Long groupId,
			Pageable pageable
	);

	@Query("""
			select groupItem.item
			from GroupItem groupItem
			where groupItem.group.id = :groupId
				and groupItem.deletedAt is null
				and groupItem.item.deletedAt is null
				and groupItem.item.id < :cursorId
			order by groupItem.item.id desc
			""")
	List<Item> findActiveItemsByGroupIdAfter(
			@Param("groupId") Long groupId,
			@Param("cursorId") Long cursorId,
			Pageable pageable
	);
}
