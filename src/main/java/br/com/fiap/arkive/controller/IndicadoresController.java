package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.response.AnimalConsultasIndicadorResponse;
import br.com.fiap.arkive.dto.response.PacientesAtivosIndicadorResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.IndicadoresService;
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
@RequestMapping("/api/indicadores")
@Profile("!local-nodb")
@Tag(name = "Indicadores Oracle", description = "Indicadores em tempo real obtidos por stored procedures Oracle.")
public class IndicadoresController {

	private final IndicadoresService indicadoresService;

	public IndicadoresController(IndicadoresService indicadoresService) {
		this.indicadoresService = indicadoresService;
	}

	@GetMapping("/animais/{animalId}/consultas")
	@Operation(summary = "Consulta os totais de consultas do animal via package Oracle")
	public ResponseEntity<AnimalConsultasIndicadorResponse> buscarConsultasAnimal(
			@PathVariable Long animalId,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(indicadoresService.buscarConsultasAnimal(animalId, principal));
	}

	@GetMapping("/pacientes-ativos")
	@Operation(summary = "Consulta o total global de pacientes ativos via package Oracle")
	public ResponseEntity<PacientesAtivosIndicadorResponse> buscarPacientesAtivos(
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(indicadoresService.buscarPacientesAtivos(principal));
	}
}
