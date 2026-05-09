package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.EspecieRequest;
import br.com.fiap.arkive.dto.response.EspecieResponse;
import br.com.fiap.arkive.service.EspecieService;
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
@RequestMapping("/api/especies")
@Profile("!local-nodb")
public class EspecieController {

	private final EspecieService especieService;

	public EspecieController(EspecieService especieService) {
		this.especieService = especieService;
	}

	@PostMapping
	public ResponseEntity<EspecieResponse> criar(@Valid @RequestBody EspecieRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(especieService.criar(request));
	}

	@GetMapping
	public Page<EspecieResponse> listar(@RequestParam(required = false) String nome, Pageable pageable) {
		return especieService.listar(nome, pageable);
	}

	@GetMapping("/{id}")
	public EspecieResponse buscarPorId(@PathVariable Long id) {
		return especieService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public EspecieResponse atualizar(@PathVariable Long id, @Valid @RequestBody EspecieRequest request) {
		return especieService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		especieService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
