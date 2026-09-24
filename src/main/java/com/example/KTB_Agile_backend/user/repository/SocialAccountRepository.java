package com.example.KTB_Agile_backend.user.repository;

import com.example.KTB_Agile_backend.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

	Optional<SocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
