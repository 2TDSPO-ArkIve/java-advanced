package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.RegistrarDispositivoRequest;
import br.com.fiap.arkive.dto.response.UsuarioDispositivoResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.UsuarioDispositivoService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes/dispositivos")
@Profile("!local-nodb")
public class UsuarioDispositivoController {

	private final UsuarioDispositivoService dispositivoService;

	public UsuarioDispositivoController(UsuarioDispositivoService dispositivoService) {
		this.dispositivoService = dispositivoService;
	}

	@PostMapping
	public ResponseEntity<UsuarioDispositivoResponse> registrar(
			@Valid @RequestBody RegistrarDispositivoRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(dispositivoService.registrar(request, principal));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> desativar(@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		dispositivoService.desativar(id, principal);
		return ResponseEntity.noContent().build();
	}
}

