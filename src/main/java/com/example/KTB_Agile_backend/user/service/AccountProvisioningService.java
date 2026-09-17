package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.response.ErrorResponse;
import com.example.KTB_Agile_backend.user.entity.SocialAccount;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.SocialAccountRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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
					HttpStatus.CONFLICT,
					"SOCIAL_ACCOUNT_CONFLICT",
					"계정 연결 정보가 충돌했습니다.",
					List.of(new ErrorResponse.Field(
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
					HttpStatus.BAD_REQUEST,
					"BAD_REQUEST",
					"요청 값이 올바르지 않습니다.",
					List.of(new ErrorResponse.Field("provider", "provider는 필수 입력값입니다."))
			);
		}
		return provider.trim().toUpperCase(Locale.ROOT);
	}

	private static ApiException authenticationFailed() {
		return new ApiException(
				HttpStatus.UNAUTHORIZED,
				"UNAUTHORIZED",
				"인증에 실패했습니다. 인가 코드가 만료되었거나 유효하지 않습니다.",
				List.of(new ErrorResponse.Field("authorizationCode", "만료되었거나 이미 사용된 인가 코드입니다."))
		);
	}
}
