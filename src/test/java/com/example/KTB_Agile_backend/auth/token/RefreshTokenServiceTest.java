package com.example.KTB_Agile_backend.auth.token;

import com.example.KTB_Agile_backend.auth.service.RefreshTokenService;
import com.example.KTB_Agile_backend.auth.service.RefreshTokenRotation;
import com.example.KTB_Agile_backend.auth.token.RefreshTokenStore;
import com.example.KTB_Agile_backend.common.util.Hashing;
import com.example.KTB_Agile_backend.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

	@Test
	void issuesRandomTokenAndStoresOnlyItsHashWithExpiration() {
		RefreshTokenStore store = mock(RefreshTokenStore.class);
		RefreshTokenService service = new RefreshTokenService(store, 14);
		User user = new User("kim");

		String token = service.issue(user);
		String sessionId = token.substring(0, token.indexOf('.'));

		assertTrue(sessionId.length() > 0);
		assertNotEquals(Hashing.sha256(token), token);
		verify(store).create(eq(sessionId), isNull(), eq(Hashing.sha256(token)), eq(Duration.ofDays(14)));
	}

	@Test
	void rotatesRefreshTokenAndKeepsItsSessionExpiry() {
		RefreshTokenStore store = mock(RefreshTokenStore.class);
		RefreshTokenService service = new RefreshTokenService(store, 14);
		User user = new User("kim");
		String token = service.issue(user);
		String sessionId = token.substring(0, token.indexOf('.'));
		when(store.rotate(eq(sessionId), eq(Hashing.sha256(token)), any()))
				.thenReturn(new RefreshTokenStore.Rotation(
						RefreshTokenStore.RotationStatus.ROTATED,
						1L,
						Duration.ofDays(3)
				));

		RefreshTokenRotation rotation = service.rotate(token);

		assertNotEquals(token, rotation.refreshToken());
		assertTrue(rotation.refreshToken().startsWith(sessionId + "."));
		assertEquals(1L, rotation.userId());
		assertEquals(Duration.ofDays(3), rotation.remainingTtl());
		verify(store).rotate(eq(sessionId), eq(Hashing.sha256(token)), eq(Hashing.sha256(rotation.refreshToken())));
	}

	@Test
	void revokesTheSessionForItsCurrentRefreshToken() {
		RefreshTokenStore store = mock(RefreshTokenStore.class);
		RefreshTokenService service = new RefreshTokenService(store, 14);
		String token = service.issue(new User("kim"));
		String sessionId = token.substring(0, token.indexOf('.'));

		service.revoke(token);

		verify(store).revoke(sessionId, Hashing.sha256(token));
	}
}
