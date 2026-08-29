package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.DiagnosticoRequest;
import br.com.fiap.arkive.dto.response.DiagnosticoResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.DiagnosticoService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnosticos")
@Profile("!local-nodb")
public class DiagnosticoController {

	private final DiagnosticoService diagnosticoService;

	public DiagnosticoController(DiagnosticoService diagnosticoService) {
		this.diagnosticoService = diagnosticoService;
	}

	@PostMapping
	public ResponseEntity<DiagnosticoResponse> criar(
			@Valid @RequestBody DiagnosticoRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(diagnosticoService.criar(request, principal));
	}

	@GetMapping
	public Page<DiagnosticoResponse> listar(
			@RequestParam(required = false) Long consultaId,
			@RequestParam(required = false) Long doencaId,
			@RequestParam(required = false) String severidade,
			@RequestParam(required = false) String confirmado,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return diagnosticoService.listarAutorizado(consultaId, doencaId, severidade, confirmado, pageable, principal);
	}

	@GetMapping("/{id}")
	public DiagnosticoResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return diagnosticoService.buscarPorIdAutorizado(id, principal);
	}

	@PutMapping("/{id}")
	public DiagnosticoResponse atualizar(
			@PathVariable Long id,
			@Valid @RequestBody DiagnosticoRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return diagnosticoService.atualizar(id, request, principal);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		diagnosticoService.excluir(id, principal);
		return ResponseEntity.noContent().build();
	}

}
