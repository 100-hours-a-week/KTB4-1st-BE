package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.ItemLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ItemLikeRepository extends JpaRepository<ItemLike, Long> {

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
}
