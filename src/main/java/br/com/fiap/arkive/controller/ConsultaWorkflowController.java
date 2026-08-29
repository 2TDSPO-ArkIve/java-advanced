package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.AtualizarNarrativaConsultaRequest;
import br.com.fiap.arkive.dto.request.CancelarConsultaRequest;
import br.com.fiap.arkive.dto.request.FinalizarConsultaRequest;
import br.com.fiap.arkive.dto.response.ClinicalSupportResponse;
import br.com.fiap.arkive.dto.response.ConsultaWorkflowResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.ClinicalSupportService;
import br.com.fiap.arkive.service.ConsultaWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultas")
@Profile("!local-nodb")
public class ConsultaWorkflowController {

	private final ConsultaWorkflowService consultaWorkflowService;
	private final ClinicalSupportService clinicalSupportService;

	public ConsultaWorkflowController(ConsultaWorkflowService consultaWorkflowService, ClinicalSupportService clinicalSupportService) {
		this.consultaWorkflowService = consultaWorkflowService;
		this.clinicalSupportService = clinicalSupportService;
	}

	@PostMapping("/{id}/iniciar")
	@Operation(summary = "Inicia uma consulta", description = "Operacao de dominio que altera uma consulta AG para EP.")
	public ConsultaWorkflowResponse iniciar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return consultaWorkflowService.iniciar(id, principal);
	}

	@PatchMapping("/{id}/narrativa")
	@Operation(summary = "Atualiza a narrativa clinica", description = "Armazena texto clinico bruto em consultas EP ou AP, sem analise por IA.")
	public ConsultaWorkflowResponse atualizarNarrativa(
			@PathVariable Long id,
			@Valid @RequestBody AtualizarNarrativaConsultaRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return consultaWorkflowService.atualizarNarrativa(id, request, principal);
	}

	@PostMapping("/{id}/finalizar")
	@Operation(summary = "Finaliza uma consulta", description = "Cria diagnostico confirmado pelo veterinario, finaliza a consulta e registra evento em uma unica transacao.")
	public ConsultaWorkflowResponse finalizar(
			@PathVariable Long id,
			@Valid @RequestBody FinalizarConsultaRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return consultaWorkflowService.finalizar(id, request, principal);
	}

	@PostMapping("/{id}/cancelar")
	@Operation(summary = "Cancela uma consulta", description = "Operacao de dominio que cancela consultas AG, EP ou AP e registra evento de jornada.")
	public ConsultaWorkflowResponse cancelar(
			@PathVariable Long id,
			@Valid @RequestBody CancelarConsultaRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return consultaWorkflowService.cancelar(id, request, principal);
	}

	@PostMapping("/{id}/suporte-clinico")
	@Operation(summary = "Gera suporte clinico", description = "Operacao de dominio que consulta o motor clinico externo uma unica vez por solicitacao explicita do veterinario e move a consulta EP para AP.")
	public ClinicalSupportResponse gerarSuporteClinico(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return clinicalSupportService.gerarSuporte(id, principal);
	}

	@GetMapping("/{id}/suporte-clinico")
	@Operation(summary = "Consulta suporte clinico persistido", description = "Retorna o suporte clinico previamente gerado sem chamar o motor externo.")
	public ClinicalSupportResponse buscarSuporteClinico(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return clinicalSupportService.buscarSuporte(id, principal);
	}
}
