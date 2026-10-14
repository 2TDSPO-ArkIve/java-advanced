package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.response.AnimalCustoResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AnimalCustoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/animais")
@Profile("!local-nodb")
@Tag(name = "Custos do paciente", description = "Custos assistenciais informativos de consultas finalizadas e prescricoes.")
public class AnimalCustoController {

	private final AnimalCustoService animalCustoService;

	public AnimalCustoController(AnimalCustoService animalCustoService) {
		this.animalCustoService = animalCustoService;
	}

	@GetMapping("/{animalId}/custos")
	@Operation(
			summary = "Consulta custos assistenciais finalizados do paciente",
			description = "Retorna consultas FI no escopo financeiro do usuario. Para veterinarios, agrega somente atendimentos proprios; responsaveis mantem a visao autorizada do paciente. Valores nao informados permanecem nulos no detalhe e contam como zero nos totais."
	)
	public ResponseEntity<AnimalCustoResponse> buscar(
			@PathVariable Long animalId,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(animalCustoService.buscar(animalId, principal));
	}
}
