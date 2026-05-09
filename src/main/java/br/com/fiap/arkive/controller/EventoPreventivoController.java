package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.EventoPreventivoRequest;
import br.com.fiap.arkive.dto.response.EventoPreventivoResponse;
import br.com.fiap.arkive.service.EventoPreventivoService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/eventos-preventivos")
@Profile("!local-nodb")
public class EventoPreventivoController {

	private final EventoPreventivoService eventoService;

	public EventoPreventivoController(EventoPreventivoService eventoService) {
		this.eventoService = eventoService;
	}

	@PostMapping
	public ResponseEntity<EventoPreventivoResponse> criar(@Valid @RequestBody EventoPreventivoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.criar(request));
	}

	@GetMapping
	public Page<EventoPreventivoResponse> listar(
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long protocoloId,
			@RequestParam(required = false) Long consultaId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String alerta,
			Pageable pageable
	) {
		return eventoService.listar(animalId, protocoloId, consultaId, status, alerta, pageable);
	}

	@GetMapping("/{id}")
	public EventoPreventivoResponse buscarPorId(@PathVariable Long id) {
		return eventoService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public EventoPreventivoResponse atualizar(@PathVariable Long id, @Valid @RequestBody EventoPreventivoRequest request) {
		return eventoService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		eventoService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
