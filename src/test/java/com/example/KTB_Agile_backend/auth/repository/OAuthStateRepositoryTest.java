package com.example.KTB_Agile_backend.auth.repository;

import com.example.KTB_Agile_backend.auth.entity.OAuthState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class OAuthStateRepositoryTest {

	@Autowired
	private OAuthStateRepository oauthStateRepository;

	@Test
	void consumesStateOnlyOnce() {
		LocalDateTime now = LocalDateTime.parse("2026-09-16T00:00:00");
		oauthStateRepository.saveAndFlush(
				new OAuthState("a".repeat(64), "KAKAO", now.plusSeconds(300))
		);

		assertEquals(1, oauthStateRepository.consumeIfValid(
				"a".repeat(64), "KAKAO", now, now.plusSeconds(1)
		));
		assertEquals(0, oauthStateRepository.consumeIfValid(
				"a".repeat(64), "KAKAO", now, now.plusSeconds(2)
		));
	}
}
