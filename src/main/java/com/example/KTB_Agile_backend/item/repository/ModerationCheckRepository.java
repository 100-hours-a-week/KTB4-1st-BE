package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.ModerationCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ModerationCheckRepository extends JpaRepository<ModerationCheck, Long> {

	@Modifying
	@Query("""
		update ModerationCheck moderationCheck
		set moderationCheck.consumedAt = :now,
		    moderationCheck.updatedAt = :now
		where moderationCheck.checkIdHash = :checkIdHash
		  and moderationCheck.userId = :userId
		  and moderationCheck.contentHash = :contentHash
		  and moderationCheck.expiresAt > :now
		  and moderationCheck.consumedAt is null
		""")
	int consumeIfValid(
			@Param("checkIdHash") String checkIdHash,
			@Param("userId") Long userId,
			@Param("contentHash") String contentHash,
			@Param("now") LocalDateTime now
	);
}
