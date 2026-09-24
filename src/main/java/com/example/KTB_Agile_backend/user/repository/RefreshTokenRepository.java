package com.example.KTB_Agile_backend.user.repository;

import com.example.KTB_Agile_backend.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHashAndDeletedAtIsNull(String tokenHash);

	@Modifying
		@Query("""
			update RefreshToken token
			set token.deletedAt = :revokedAt,
			    token.updatedAt = :revokedAt
			where token.user.id = :userId
			  and token.deletedAt is null
			""")
	int revokeAllByUserId(
			@Param("userId") Long userId,
			@Param("revokedAt") LocalDateTime revokedAt
	);
}
