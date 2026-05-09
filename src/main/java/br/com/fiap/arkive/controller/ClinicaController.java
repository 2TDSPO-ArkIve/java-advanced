package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.ClinicaRequest;
import br.com.fiap.arkive.dto.response.ClinicaResponse;
import br.com.fiap.arkive.service.ClinicaService;
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
@RequestMapping("/api/clinicas")
@Profile("!local-nodb")
public class ClinicaController {

	private final ClinicaService clinicaService;

	public ClinicaController(ClinicaService clinicaService) {
		this.clinicaService = clinicaService;
	}

	@PostMapping
	public ResponseEntity<ClinicaResponse> criar(@Valid @RequestBody ClinicaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(clinicaService.criar(request));
	}

	@GetMapping
	public Page<ClinicaResponse> listar(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) String ativo,
			Pageable pageable
	) {
		return clinicaService.listar(nome, ativo, pageable);
	}

	@GetMapping("/{id}")
	public ClinicaResponse buscarPorId(@PathVariable Long id) {
		return clinicaService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public ClinicaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ClinicaRequest request) {
		return clinicaService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		clinicaService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
