package com.example.KTB_Agile_backend.image.repository;

import com.example.KTB_Agile_backend.image.entity.Image;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select image
			from Image image
			where image.id in :imageIds
			order by image.id asc
			""")
	List<Image> findAllForUpdateByIdIn(@Param("imageIds") Collection<Long> imageIds);

	@Query("""
			select image
			from Image image
			where image.item.id in :itemIds
			order by image.item.id asc, image.id asc
			""")
	List<Image> findAllByItemIdsOrderByItemIdAndId(@Param("itemIds") Collection<Long> itemIds);

	List<Image> findAllByItem_IdOrderByIdAsc(Long itemId);

	boolean existsByObjectKeyIn(Collection<String> objectKeys);
}
