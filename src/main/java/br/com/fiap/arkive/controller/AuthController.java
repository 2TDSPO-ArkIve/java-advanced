package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.AlterarSenhaApiRequest;
import br.com.fiap.arkive.dto.response.AuthMeResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Profile("!local-nodb")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/me")
	public AuthMeResponse me(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return authService.me(principal);
	}

	@PostMapping("/change-password")
	public ResponseEntity<Void> alterarSenha(
			@Valid @RequestBody AlterarSenhaApiRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		authService.alterarSenha(principal, request.novaSenha());
		return ResponseEntity.noContent().build();
	}
}
