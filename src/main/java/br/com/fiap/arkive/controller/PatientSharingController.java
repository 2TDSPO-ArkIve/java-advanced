package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.CreatePatientShareRequest;
import br.com.fiap.arkive.dto.request.ShareTokenRequest;
import br.com.fiap.arkive.dto.response.*;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.PatientAccessService;
import br.com.fiap.arkive.service.PatientShareService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Profile("!local-nodb")
public class PatientSharingController {

	private final PatientShareService shareService;
	private final PatientAccessService accessService;

	public PatientSharingController(PatientShareService shareService, PatientAccessService accessService) {
		this.shareService = shareService;
		this.accessService = accessService;
	}

	@PostMapping("/api/animais/{animalId}/compartilhamentos")
	public ResponseEntity<PatientShareCreatedResponse> create(@PathVariable Long animalId,
			@Valid @RequestBody CreatePatientShareRequest request, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResponseEntity.status(HttpStatus.CREATED).body(shareService.create(animalId, request, principal));
	}

	@GetMapping("/api/animais/{animalId}/compartilhamentos")
	public List<PatientShareResponse> listInvitations(@PathVariable Long animalId,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return shareService.list(animalId, principal);
	}

	@PostMapping("/api/compartilhamentos/preview")
	public PatientSharePreviewResponse preview(@Valid @RequestBody ShareTokenRequest request) {
		return shareService.preview(request.token());
	}

	@PostMapping("/api/compartilhamentos/redeem")
	public PatientAccessResponse redeem(@Valid @RequestBody ShareTokenRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return shareService.redeem(request.token(), principal);
	}

	@DeleteMapping("/api/compartilhamentos/{invitationId}")
	public PatientShareResponse revokeInvitation(@PathVariable Long invitationId,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return shareService.revoke(invitationId, principal);
	}

	@GetMapping("/api/animais/{animalId}/acessos")
	public List<PatientAccessResponse> listAccess(@PathVariable Long animalId,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return accessService.list(animalId, principal);
	}

	@DeleteMapping("/api/animais/{animalId}/acessos/{accessId}")
	public PatientAccessResponse revokeAccess(@PathVariable Long animalId, @PathVariable Long accessId,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return accessService.revoke(animalId, accessId, principal);
	}
}
