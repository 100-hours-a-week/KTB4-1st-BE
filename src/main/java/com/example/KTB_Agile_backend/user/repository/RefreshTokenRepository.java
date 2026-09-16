package com.example.KTB_Agile_backend.user.repository;

import com.example.KTB_Agile_backend.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHashAndDeletedAtIsNull(String tokenHash);
}
