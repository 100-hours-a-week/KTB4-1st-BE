package com.example.KTB_Agile_backend.auth.repository;

import com.example.KTB_Agile_backend.auth.entity.OAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface OAuthStateRepository extends JpaRepository<OAuthState, Long> {

	@Transactional
	@Modifying
		@Query("""
			update OAuthState oauthState
			set oauthState.consumedAt = :consumedAt,
			    oauthState.updatedAt = :consumedAt
			where oauthState.stateHash = :stateHash
			  and oauthState.provider = :provider
			  and oauthState.expiresAt > :now
			  and oauthState.consumedAt is null
			""")
	int consumeIfValid(
			@Param("stateHash") String stateHash,
			@Param("provider") String provider,
			@Param("now") LocalDateTime now,
			@Param("consumedAt") LocalDateTime consumedAt
	);
}
