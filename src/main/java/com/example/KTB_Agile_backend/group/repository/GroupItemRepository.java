package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GroupItemRepository extends JpaRepository<GroupItem, Long> {

	@Query("""
				select new com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection(
					item.id,
					item.title,
					item.content,
					item.quantity,
					owner.id,
					owner.nickname,
					item.itemState,
					item.createdAt
				)
				from GroupItem groupItem
				join groupItem.item item
				join item.user owner
				where groupItem.group.id = :groupId
					and groupItem.deletedAt is null
					and item.deletedAt is null
				order by item.id desc
				""")
	List<ItemSummaryProjection> findActiveItemSummariesByGroupId(
			@Param("groupId") Long groupId,
			Pageable pageable
	);

	@Query("""
				select new com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection(
					item.id,
					item.title,
					item.content,
					item.quantity,
					owner.id,
					owner.nickname,
					item.itemState,
					item.createdAt
				)
				from GroupItem groupItem
				join groupItem.item item
				join item.user owner
				where groupItem.group.id = :groupId
					and groupItem.deletedAt is null
					and item.deletedAt is null
					and item.id < :cursorId
				order by item.id desc
				""")
	List<ItemSummaryProjection> findActiveItemSummariesByGroupIdAfter(
			@Param("groupId") Long groupId,
			@Param("cursorId") Long cursorId,
			Pageable pageable
	);

	@Query("""
			select distinct new com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection(
				item.id,
				item.title,
				item.content,
				item.quantity,
				owner.id,
				owner.nickname,
				item.itemState,
				item.createdAt
			)
			from GroupItem groupItem
			join groupItem.item item
			join item.user owner
			where groupItem.deletedAt is null
				and groupItem.group.deletedAt is null
				and item.deletedAt is null
				and exists (
					select groupMember.id
					from GroupMember groupMember
					where groupMember.group.id = groupItem.group.id
						and groupMember.user.id = :userId
						and groupMember.status = :activeStatus
				)
				and (
					:keyword = ''
					or item.title like concat('%', :keyword, '%') escape '!'
					or item.content like concat('%', :keyword, '%') escape '!'
				)
			order by item.id desc
			""")
	List<ItemSummaryProjection> findActiveItemSummariesForSearch(
			@Param("userId") Long userId,
			@Param("activeStatus") GroupMemberStatus activeStatus,
			@Param("keyword") String keyword,
			Pageable pageable
	);

	@Query("""
			select distinct new com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection(
				item.id,
				item.title,
				item.content,
				item.quantity,
				owner.id,
				owner.nickname,
				item.itemState,
				item.createdAt
			)
			from GroupItem groupItem
			join groupItem.item item
			join item.user owner
			where groupItem.deletedAt is null
				and groupItem.group.deletedAt is null
				and item.deletedAt is null
				and exists (
					select groupMember.id
					from GroupMember groupMember
					where groupMember.group.id = groupItem.group.id
						and groupMember.user.id = :userId
						and groupMember.status = :activeStatus
				)
				and (
					:keyword = ''
					or item.title like concat('%', :keyword, '%') escape '!'
					or item.content like concat('%', :keyword, '%') escape '!'
				)
				and item.id < :cursorId
			order by item.id desc
			""")
	List<ItemSummaryProjection> findActiveItemSummariesForSearchAfter(
			@Param("userId") Long userId,
			@Param("activeStatus") GroupMemberStatus activeStatus,
			@Param("keyword") String keyword,
			@Param("cursorId") Long cursorId,
			Pageable pageable
	);

	@Query("""
			select groupItem
			from GroupItem groupItem
			join fetch groupItem.group group
			where groupItem.item.id = :itemId
				and groupItem.deletedAt is null
				and group.deletedAt is null
			order by group.id asc
			""")
	List<GroupItem> findActiveGroupItemsByItemId(@Param("itemId") Long itemId);

	@Query("""
		select groupItem
		from GroupItem groupItem
		join fetch groupItem.group group
		where groupItem.item.id in :itemIds
			and groupItem.deletedAt is null
			and groupItem.item.deletedAt is null
			and group.deletedAt is null
		order by groupItem.item.id asc, group.id asc
		""")
	List<GroupItem> findActiveGroupItemsByItemIds(@Param("itemIds") Collection<Long> itemIds);

	@Query("""
			select groupItem
			from GroupItem groupItem
			join fetch groupItem.group group
			where groupItem.item.id = :itemId
			order by group.id asc
			""")
	List<GroupItem> findAllByItemId(@Param("itemId") Long itemId);
}
