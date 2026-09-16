package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.user.entity.SocialAccount;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.SocialAccountRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountProvisioningService {

	private final UserRepository userRepository;
	private final SocialAccountRepository socialAccountRepository;

	@Transactional
	public AccountResult findOrCreate(OAuthUserInfo userInfo) {
		String provider = normalizeProvider(userInfo.provider());
		SocialAccount socialAccount = socialAccountRepository
				.findByProviderAndProviderUserId(provider, userInfo.providerUserId())
				.orElse(null);

		if (socialAccount != null) {
			User user = userRepository.findActiveById(socialAccount.getUser().getId())
					.orElseThrow(() -> new IllegalStateException("user is not active"));
			socialAccount.recordLogin();
			return new AccountResult(user, false);
		}

		User user = userRepository.save(
				new User(userInfo.nickname(), userInfo.profileImageUrl())
		);
		socialAccountRepository.saveAndFlush(
				new SocialAccount(user, provider, userInfo.providerUserId())
		);

		return new AccountResult(user, true);
	}

	private static String normalizeProvider(String provider) {
		if (provider == null || provider.isBlank()) {
			throw new IllegalArgumentException("provider must not be blank");
		}
		return provider.trim().toUpperCase(Locale.ROOT);
	}
}
