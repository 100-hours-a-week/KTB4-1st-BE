package com.example.KTB_Agile_backend.user.controller;

import com.example.KTB_Agile_backend.user.service.AccountWithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final AccountWithdrawalService accountWithdrawalService;

	@DeleteMapping
	public ResponseEntity<Void> withdraw(Authentication authentication) {
		accountWithdrawalService.withdraw(Long.valueOf(authentication.getName()));
		return ResponseEntity.noContent().build();
	}
}
