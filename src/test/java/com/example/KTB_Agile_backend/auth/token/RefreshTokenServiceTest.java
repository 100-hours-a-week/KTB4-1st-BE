package com.example.KTB_Agile_backend.auth.token;

import com.example.KTB_Agile_backend.auth.service.Hashing;
import com.example.KTB_Agile_backend.auth.service.RefreshTokenService;
import com.example.KTB_Agile_backend.user.entity.RefreshToken;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

	@Test
	void issuesRandomTokenAndStoresOnlyItsHashWithExpiration() {
		RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
		RefreshTokenService service = new RefreshTokenService(repository, 14);
		User user = new User("kim");
		LocalDateTime beforeIssue = LocalDateTime.now().plusDays(14);

		String token = service.issue(user);

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(repository).save(captor.capture());
		RefreshToken savedToken = captor.getValue();
		assertSame(user, savedToken.getUser());
		assertEquals(Hashing.sha256(token), savedToken.getTokenHash());
		assertFalse(token.equals(savedToken.getTokenHash()));
		assertFalse(savedToken.getExpiresAt().isBefore(beforeIssue));
	}

	@Test
	void returnsUserForValidRefreshToken() {
		RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
		RefreshTokenService service = new RefreshTokenService(repository, 14);
		User user = new User("kim");
		RefreshToken savedToken = new RefreshToken(user, Hashing.sha256("refresh-token"), LocalDateTime.now().plusDays(1));
		when(repository.findByTokenHashAndDeletedAtIsNull(Hashing.sha256("refresh-token")))
				.thenReturn(Optional.of(savedToken));

		assertSame(user, service.requireValidUser("refresh-token"));
	}

	@Test
	void revokesStoredRefreshToken() {
		RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
		RefreshTokenService service = new RefreshTokenService(repository, 14);
		RefreshToken savedToken = new RefreshToken(
				new User("kim"),
				Hashing.sha256("refresh-token"),
				LocalDateTime.now().plusDays(1)
		);
		when(repository.findByTokenHashAndDeletedAtIsNull(Hashing.sha256("refresh-token")))
				.thenReturn(Optional.of(savedToken));

		service.revoke("refresh-token");

		verify(repository).save(savedToken);
		assertNotNull(savedToken.getDeletedAt());
	}
}
