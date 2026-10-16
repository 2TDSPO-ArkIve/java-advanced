package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.CreatePatientAccessRequest;
import br.com.fiap.arkive.dto.response.PatientAccessRequestResponse;
import br.com.fiap.arkive.entity.PatientAccessRequestStatus;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.PatientAccessRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/solicitacoes-acesso")
@Profile("!local-nodb")
@Tag(name = "Solicitações de acesso", description = "Pedido veterinário e decisão por responsável atual do paciente.")
public class PatientAccessRequestController {

	private final PatientAccessRequestService service;

	public PatientAccessRequestController(PatientAccessRequestService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Solicita acesso pelo ArkIve ID", description = "O ArkIve ID identifica o paciente, mas não concede acesso sem aprovação de responsável vigente.")
	public ResponseEntity<PatientAccessRequestResponse> criar(
			@Valid @RequestBody CreatePatientAccessRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request, principal));
	}

	@GetMapping
	@Operation(summary = "Lista solicitações do contexto autenticado")
	public Page<PatientAccessRequestResponse> listar(
			@RequestParam(required = false) PatientAccessRequestStatus status,
			@PageableDefault(sort = {"requestedAt", "id"}, direction = DESC) Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return service.listar(status, pageable, principal);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta uma solicitação autorizada")
	public PatientAccessRequestResponse buscar(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return service.buscar(id, principal);
	}

	@PostMapping("/{id}/aprovar")
	@Operation(summary = "Aprova acesso veterinário", description = "Exige vínculo AnimalResponsavel ativo e vigente no momento da decisão.")
	public PatientAccessRequestResponse aprovar(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return service.aprovar(id, principal);
	}

	@PostMapping("/{id}/rejeitar")
	@Operation(summary = "Rejeita acesso veterinário", description = "A rejeição não cria nem revoga outros acessos.")
	public PatientAccessRequestResponse rejeitar(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return service.rejeitar(id, principal);
	}
}
