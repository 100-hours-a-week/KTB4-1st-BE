package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.user.entity.SocialAccount;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.SocialAccountRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountProvisioningServiceTest {

	@Test
	void returnsExistingActiveUser() {
		UserRepository userRepository = mock(UserRepository.class);
		SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
		AccountProvisioningService service = new AccountProvisioningService(
				userRepository,
				socialAccountRepository
		);
		User user = mock(User.class);
		when(user.getId()).thenReturn(1L);
		SocialAccount socialAccount = new SocialAccount(user, "KAKAO", "provider-user-1");
		when(socialAccountRepository.findByProviderAndProviderUserId("KAKAO", "provider-user-1"))
				.thenReturn(Optional.of(socialAccount));
		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

		AccountResult result = service.findOrCreate(
				new OAuthUserInfo("kakao", "provider-user-1", "kim", null)
		);

		assertSame(user, result.user());
		assertFalse(result.newUser());
		verify(userRepository, never()).save(any());
		verify(socialAccountRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsLinkedInactiveUser() {
		UserRepository userRepository = mock(UserRepository.class);
		SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
		AccountProvisioningService service = new AccountProvisioningService(
				userRepository,
				socialAccountRepository
		);
		User user = mock(User.class);
		when(user.getId()).thenReturn(1L);
		when(socialAccountRepository.findByProviderAndProviderUserId("KAKAO", "provider-user-1"))
				.thenReturn(Optional.of(new SocialAccount(user, "KAKAO", "provider-user-1")));
		when(userRepository.findActiveById(1L)).thenReturn(Optional.empty());

		assertThrows(ApiException.class, () -> service.findOrCreate(
				new OAuthUserInfo("KAKAO", "provider-user-1", "kim", null)
		));
		verify(userRepository, never()).save(any());
	}

	@Test
	void createsUserAndSocialAccountWhenNoLinkExists() {
		UserRepository userRepository = mock(UserRepository.class);
		SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
		AccountProvisioningService service = new AccountProvisioningService(
				userRepository,
				socialAccountRepository
		);
		when(socialAccountRepository.findByProviderAndProviderUserId("KAKAO", "provider-user-1"))
				.thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AccountResult result = service.findOrCreate(
				new OAuthUserInfo("KAKAO", "provider-user-1", "kim", "profile-image")
		);

		assertTrue(result.newUser());
		verify(userRepository).save(any(User.class));
		verify(socialAccountRepository).saveAndFlush(any(SocialAccount.class));
	}

	@Test
	void returnsConflictWhenSocialAccountIsLinkedConcurrently() {
		UserRepository userRepository = mock(UserRepository.class);
		SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
		AccountProvisioningService service = new AccountProvisioningService(
				userRepository,
				socialAccountRepository
		);
		when(socialAccountRepository.findByProviderAndProviderUserId("KAKAO", "provider-user-1"))
				.thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(socialAccountRepository.saveAndFlush(any(SocialAccount.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate social account"));

		ApiException exception = assertThrows(ApiException.class, () -> service.findOrCreate(
				new OAuthUserInfo("KAKAO", "provider-user-1", "kim", null)
		));

		assertEquals(HttpStatus.CONFLICT, exception.status());
		assertEquals("SOCIAL_ACCOUNT_CONFLICT", exception.error().code());
	}
}
