package com.example.KTB_Agile_backend.migration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@ConditionalOnProperty(name = "migration.enabled", havingValue = "true")
public class MigrationAdminAccessFilter extends OncePerRequestFilter {

	private static final String ADMIN_PATH = "/internal/admin/migration/";

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		if (!request.getRequestURI().startsWith(ADMIN_PATH) || isLoopback(request.getRemoteAddr())) {
			filterChain.doFilter(request, response);
			return;
		}
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "migration admin is local-only");
	}

	private static boolean isLoopback(String address) {
		try {
			return InetAddress.getByName(address).isLoopbackAddress();
		} catch (UnknownHostException exception) {
			return false;
		}
	}
}
