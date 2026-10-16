package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.response.VeterinarioCustoResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.VeterinarioCustoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinarios/me")
@Profile("!local-nodb")
@Tag(name = "Custos do veterinario", description = "Custos assistenciais pertencentes ao veterinario autenticado.")
public class VeterinarioCustoController {

	private final VeterinarioCustoService veterinarioCustoService;

	public VeterinarioCustoController(VeterinarioCustoService veterinarioCustoService) {
		this.veterinarioCustoService = veterinarioCustoService;
	}

	@GetMapping("/custos")
	@Operation(
			summary = "Consulta os custos do veterinario autenticado",
			description = "Agrega somente consultas finalizadas pertencentes ao veterinario identificado pelo principal autenticado."
	)
	public ResponseEntity<VeterinarioCustoResponse> buscar(
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(veterinarioCustoService.buscarCustosAutenticados(principal));
	}
}
