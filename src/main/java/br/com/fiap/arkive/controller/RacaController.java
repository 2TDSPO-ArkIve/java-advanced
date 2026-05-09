package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.RacaRequest;
import br.com.fiap.arkive.dto.response.RacaResponse;
import br.com.fiap.arkive.service.RacaService;
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
@RequestMapping("/api/racas")
@Profile("!local-nodb")
public class RacaController {

	private final RacaService racaService;

	public RacaController(RacaService racaService) {
		this.racaService = racaService;
	}

	@PostMapping
	public ResponseEntity<RacaResponse> criar(@Valid @RequestBody RacaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(racaService.criar(request));
	}

	@GetMapping
	public Page<RacaResponse> listar(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) Long especieId,
			Pageable pageable
	) {
		return racaService.listar(nome, especieId, pageable);
	}

	@GetMapping("/{id}")
	public RacaResponse buscarPorId(@PathVariable Long id) {
		return racaService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public RacaResponse atualizar(@PathVariable Long id, @Valid @RequestBody RacaRequest request) {
		return racaService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		racaService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
