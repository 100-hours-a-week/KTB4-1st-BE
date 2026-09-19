package com.example.KTB_Agile_backend.auth.state;

import com.example.KTB_Agile_backend.auth.entity.OAuthState;
import com.example.KTB_Agile_backend.auth.repository.OAuthStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class RdbOAuthStateStore implements OAuthStateStore {

	private final OAuthStateRepository oauthStateRepository;

	@Override
	public void save(String stateHash, String provider, LocalDateTime expiresAt) {
		oauthStateRepository.save(new OAuthState(stateHash, provider, expiresAt));
	}

	@Override
	public boolean consumeIfValid(String stateHash, String provider, LocalDateTime now) {
		return oauthStateRepository.consumeIfValid(stateHash, provider, now, now) == 1;
	}
}
