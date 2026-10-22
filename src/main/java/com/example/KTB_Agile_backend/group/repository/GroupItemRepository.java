package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
				join GroupMember membership on membership.group = groupItem.group
					and membership.user = owner
				where groupItem.group.id = :groupId
					and groupItem.deletedAt is null
					and item.deletedAt is null
					and owner.userStatus = :activeUserStatus
					and membership.status = :activeMemberStatus
				order by item.id desc
				""")
	List<ItemSummaryProjection> findActiveItemSummariesByGroupId(
			@Param("groupId") Long groupId,
			@Param("activeUserStatus") UserStatus activeUserStatus,
			@Param("activeMemberStatus") GroupMemberStatus activeMemberStatus,
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
				join GroupMember membership on membership.group = groupItem.group
					and membership.user = owner
				where groupItem.group.id = :groupId
					and groupItem.deletedAt is null
					and item.deletedAt is null
					and owner.userStatus = :activeUserStatus
					and membership.status = :activeMemberStatus
					and item.id < :cursorId
				order by item.id desc
				""")
	List<ItemSummaryProjection> findActiveItemSummariesByGroupIdAfter(
			@Param("groupId") Long groupId,
			@Param("cursorId") Long cursorId,
			@Param("activeUserStatus") UserStatus activeUserStatus,
			@Param("activeMemberStatus") GroupMemberStatus activeMemberStatus,
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
			join GroupMember membership on membership.group = groupItem.group
				and membership.user = owner
			where groupItem.deletedAt is null
				and groupItem.group.deletedAt is null
				and groupItem.group.id = :searchGroupId
				and item.deletedAt is null
				and owner.userStatus = :activeUserStatus
				and membership.status = :activeMemberStatus
				and (:itemState is null or item.itemState = :itemState)
				and (
					:keyword = ''
					or item.title like concat('%', :keyword, '%') escape '!'
					or item.content like concat('%', :keyword, '%') escape '!'
				)
			order by item.id desc
			""")
	List<ItemSummaryProjection> findActiveItemSummariesForSearch(
			@Param("searchGroupId") Long groupId,
			@Param("keyword") String keyword,
			@Param("itemState") ItemState itemState,
			@Param("activeUserStatus") UserStatus activeUserStatus,
			@Param("activeMemberStatus") GroupMemberStatus activeMemberStatus,
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
			join GroupMember membership on membership.group = groupItem.group
				and membership.user = owner
			where groupItem.deletedAt is null
				and groupItem.group.deletedAt is null
				and groupItem.group.id = :searchGroupId
				and item.deletedAt is null
				and owner.userStatus = :activeUserStatus
				and membership.status = :activeMemberStatus
				and (:itemState is null or item.itemState = :itemState)
				and (
					:keyword = ''
					or item.title like concat('%', :keyword, '%') escape '!'
					or item.content like concat('%', :keyword, '%') escape '!'
				)
				and item.id < :cursorId
			order by item.id desc
			""")
	List<ItemSummaryProjection> findActiveItemSummariesForSearchAfter(
			@Param("searchGroupId") Long groupId,
			@Param("keyword") String keyword,
			@Param("cursorId") Long cursorId,
			@Param("itemState") ItemState itemState,
			@Param("activeUserStatus") UserStatus activeUserStatus,
			@Param("activeMemberStatus") GroupMemberStatus activeMemberStatus,
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
			join GroupMember membership on membership.group = groupItem.group
				and membership.user = owner
			where groupItem.group.id = :groupId
				and groupItem.deletedAt is null
				and groupItem.group.deletedAt is null
				and item.deletedAt is null
				and item.itemState = :availableState
				and item.quantity > 0
				and item.unitPrice <= :maxUnitPrice
				and owner.id <> :userId
				and owner.userStatus = :activeUserStatus
				and membership.status = :activeMemberStatus
				and not exists (
					select request.id
					from ExchangeRequest request
					where request.requester.id = :userId
						and request.item = item
						and request.requestedStatus = :pendingStatus
				)
			order by item.createdAt desc, item.id desc
			""")
	List<ItemSummaryProjection> findSmartSearchCandidates(
			@Param("groupId") Long groupId,
			@Param("userId") Long userId,
			@Param("maxUnitPrice") BigDecimal maxUnitPrice,
			@Param("availableState") ItemState availableState,
			@Param("activeUserStatus") UserStatus activeUserStatus,
			@Param("activeMemberStatus") GroupMemberStatus activeMemberStatus,
			@Param("pendingStatus") ExchangeRequestStatus pendingStatus,
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
