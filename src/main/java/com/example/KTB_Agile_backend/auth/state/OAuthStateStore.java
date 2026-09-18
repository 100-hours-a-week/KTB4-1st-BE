package com.example.KTB_Agile_backend.auth.state;

import java.time.LocalDateTime;

public interface OAuthStateStore {

	void save(String stateHash, String provider, LocalDateTime expiresAt);

	boolean consumeIfValid(String stateHash, String provider, LocalDateTime now);
}
