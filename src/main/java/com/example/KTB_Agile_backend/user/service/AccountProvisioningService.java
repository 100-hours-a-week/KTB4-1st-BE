package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.auth.exception.AuthErrorCode;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorDetail;
import com.example.KTB_Agile_backend.user.exception.UserErrorCode;
import com.example.KTB_Agile_backend.user.entity.SocialAccount;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.SocialAccountRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
					.orElseThrow(AccountProvisioningService::authenticationFailed);
			socialAccount.recordLogin();
			return new AccountResult(user, false);
		}

		User user = userRepository.save(
				new User(userInfo.nickname(), userInfo.profileImageUrl())
		);
		try {
			socialAccountRepository.saveAndFlush(
					new SocialAccount(user, provider, userInfo.providerUserId())
			);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(
					UserErrorCode.SOCIAL_ACCOUNT_CONFLICT,
					List.of(new ErrorDetail(
							"socialAccount",
							"해당 계정이 다른 사용자와 연결되어 있습니다."
					)),
					exception
			);
		}

		return new AccountResult(user, true);
	}

	private static String normalizeProvider(String provider) {
		if (provider == null || provider.isBlank()) {
			throw new ApiException(
					AuthErrorCode.AUTH_PROVIDER_REQUIRED,
					List.of(new ErrorDetail("provider", "provider는 필수 입력값입니다."))
			);
		}
		return provider.trim().toUpperCase(Locale.ROOT);
	}

	private static ApiException authenticationFailed() {
		return new ApiException(AuthErrorCode.AUTHENTICATION_FAILED);
	}
}
