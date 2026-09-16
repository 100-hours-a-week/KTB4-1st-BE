package com.example.KTB_Agile_backend.auth.state;

import java.time.Instant;

public interface OAuthStateStore {

	void save(String stateHash, String provider, Instant expiresAt);

	boolean consumeIfValid(String stateHash, String provider, Instant now);
}
