package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.service.ConsultaService;
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
@RequestMapping("/api/consultas")
@Profile("!local-nodb")
public class ConsultaController {

	private final ConsultaService consultaService;

	public ConsultaController(ConsultaService consultaService) {
		this.consultaService = consultaService;
	}

	@PostMapping
	public ResponseEntity<ConsultaResponse> criar(@Valid @RequestBody ConsultaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.criar(request));
	}

	@GetMapping
	public Page<ConsultaResponse> listar(
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long veterinarioId,
			@RequestParam(required = false) Long clinicaId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String modalidade,
			Pageable pageable
	) {
		return consultaService.listar(animalId, veterinarioId, clinicaId, status, modalidade, pageable);
	}

	@GetMapping("/{id}")
	public ConsultaResponse buscarPorId(@PathVariable Long id) {
		return consultaService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public ConsultaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ConsultaRequest request) {
		return consultaService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		consultaService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
