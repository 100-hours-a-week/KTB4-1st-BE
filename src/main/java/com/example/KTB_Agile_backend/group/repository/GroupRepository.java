package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.group.entity.Group;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Group> findByIdAndDeletedAtIsNull(Long groupId);

	List<Group> findByDeletedAtIsNullAndGroupNameContainingOrderByIdDesc(
			String groupName,
			Pageable pageable
	);

	List<Group> findByDeletedAtIsNullAndGroupNameContainingAndIdLessThanOrderByIdDesc(
			String groupName,
			Long cursorId,
			Pageable pageable
	);
}
