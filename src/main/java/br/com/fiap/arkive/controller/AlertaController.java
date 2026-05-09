package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.AlertaRequest;
import br.com.fiap.arkive.dto.response.AlertaResponse;
import br.com.fiap.arkive.service.AlertaService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertas")
@Profile("!local-nodb")
public class AlertaController {

	private final AlertaService alertaService;

	public AlertaController(AlertaService alertaService) {
		this.alertaService = alertaService;
	}

	@PostMapping
	public ResponseEntity<AlertaResponse> criar(@Valid @RequestBody AlertaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(alertaService.criar(request));
	}

	@GetMapping
	public Page<AlertaResponse> listar(
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long responsavelId,
			@RequestParam(required = false) Long clinicaId,
			@RequestParam(required = false) Long eventoPreventivoId,
			@RequestParam(required = false) String tipo,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String canal,
			Pageable pageable
	) {
		return alertaService.listar(animalId, responsavelId, clinicaId, eventoPreventivoId, tipo, status, canal, pageable);
	}

	@GetMapping("/{id}")
	public AlertaResponse buscarPorId(@PathVariable Long id) {
		return alertaService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public AlertaResponse atualizar(@PathVariable Long id, @Valid @RequestBody AlertaRequest request) {
		return alertaService.atualizar(id, request);
	}

	@PatchMapping("/{id}/ler")
	public AlertaResponse marcarComoLido(@PathVariable Long id) {
		return alertaService.marcarComoLido(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		alertaService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
