package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemLike;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemLikeRepository extends JpaRepository<ItemLike, Long> {

	@Query("""
			select item
			from ItemLike itemLike
			join itemLike.item item
			where itemLike.user.id = ?1
				and item.deletedAt is null
			order by item.id desc
			""")
	List<Item> findLikedItemsByUserId(Long userId, Pageable pageable);

	@Query("""
			select item
			from ItemLike itemLike
			join itemLike.item item
			where itemLike.user.id = ?1
				and item.deletedAt is null
				and item.id < ?2
			order by item.id desc
			""")
	List<Item> findLikedItemsByUserIdAfter(
			Long userId,
			Long cursorId,
			Pageable pageable
	);

	@Query("""
			select itemLike
			from ItemLike itemLike
			where itemLike.item.id in :itemIds
				and itemLike.user.id = :userId
			""")
	List<ItemLike> findAllByItemIdsAndUserId(
			@Param("itemIds") Collection<Long> itemIds,
			@Param("userId") Long userId
	);

	boolean existsByItem_IdAndUser_Id(Long itemId, Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select itemLike
			from ItemLike itemLike
			where itemLike.item.id = :itemId
				and itemLike.user.id = :userId
			""")
	Optional<ItemLike> findByItemAndUserForUpdate(
			@Param("itemId") Long itemId,
			@Param("userId") Long userId
	);
}
