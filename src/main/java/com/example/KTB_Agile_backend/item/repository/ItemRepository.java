package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemPriceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface ItemRepository extends JpaRepository<Item, Long> {
	@Modifying
	@Query("update Item item set item.unitPrice = :unitPrice, item.minUnitPrice = :minUnitPrice, "
			+ "item.maxUnitPrice = :maxUnitPrice, item.priceStatus = com.example.KTB_Agile_backend.item.entity.ItemPriceStatus.SUCCESS "
			+ "where item.id = :itemId and item.priceRequestId = :requestId "
			+ "and item.priceStatus = com.example.KTB_Agile_backend.item.entity.ItemPriceStatus.PENDING")
	int completePriceRequest(@Param("itemId") Long itemId, @Param("requestId") String requestId,
			@Param("unitPrice") Long unitPrice, @Param("minUnitPrice") Long minUnitPrice,
			@Param("maxUnitPrice") Long maxUnitPrice);

	@Modifying
	@Query("update Item item set item.priceStatus = com.example.KTB_Agile_backend.item.entity.ItemPriceStatus.FAILED "
			+ "where item.id = :itemId and item.priceRequestId = :requestId "
			+ "and item.priceStatus = com.example.KTB_Agile_backend.item.entity.ItemPriceStatus.PENDING")
	int failPriceRequest(@Param("itemId") Long itemId, @Param("requestId") String requestId);

	boolean existsByIdAndPriceRequestIdAndPriceStatus(Long itemId, String requestId, ItemPriceStatus priceStatus);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select item from Item item where item.id = :itemId and item.priceRequestId = :requestId "
			+ "and item.priceStatus = com.example.KTB_Agile_backend.item.entity.ItemPriceStatus.PENDING")
	Optional<Item> findPendingPriceRequestForUpdate(@Param("itemId") Long itemId,
			@Param("requestId") String requestId);

	Optional<Item> findByIdAndDeletedAtIsNull(Long itemId);

	List<Item> findAllByIdInAndDeletedAtIsNull(Collection<Long> itemIds);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select item from Item item where item.id in :itemIds order by item.id")
	List<Item> findAllForExchange(@Param("itemIds") Collection<Long> itemIds);

	List<Item> findAllByDeletedAtIsNullOrderByIdDesc(Pageable pageable);

	List<Item> findAllByDeletedAtIsNullAndIdLessThanOrderByIdDesc(
			Long cursorId,
			Pageable pageable
	);

	List<Item> findAllByUser_IdAndDeletedAtIsNullOrderByIdDesc(Long userId, Pageable pageable);

	List<Item> findAllByUser_IdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(
			Long userId,
			Long cursorId,
			Pageable pageable
	);

	@Query("""
			select item
			from Item item
			where item.user.id = :userId
				and item.deletedAt is null
				and exists (
					select groupItem.id
					from GroupItem groupItem
					where groupItem.item = item
						and groupItem.group.id = :groupId
						and groupItem.deletedAt is null
						and groupItem.group.deletedAt is null
				)
			order by item.id desc
			""")
	List<Item> findAllByUserIdAndGroupIdOrderByIdDesc(
			@Param("userId") Long userId,
			@Param("groupId") Long groupId,
			Pageable pageable
	);

	@Query("""
			select item
			from Item item
			where item.user.id = :userId
				and item.deletedAt is null
				and item.id < :cursorId
				and exists (
					select groupItem.id
					from GroupItem groupItem
					where groupItem.item = item
						and groupItem.group.id = :groupId
						and groupItem.deletedAt is null
						and groupItem.group.deletedAt is null
				)
			order by item.id desc
			""")
	List<Item> findAllByUserIdAndGroupIdAndIdLessThanOrderByIdDesc(
			@Param("userId") Long userId,
			@Param("groupId") Long groupId,
			@Param("cursorId") Long cursorId,
			Pageable pageable
	);
}
