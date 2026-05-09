package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.AdesaoPrescricaoRequest;
import br.com.fiap.arkive.dto.response.AdesaoPrescricaoResponse;
import br.com.fiap.arkive.service.AdesaoPrescricaoService;
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
@RequestMapping("/api/adesoes-prescricao")
@Profile("!local-nodb")
public class AdesaoPrescricaoController {

	private final AdesaoPrescricaoService adesaoPrescricaoService;

	public AdesaoPrescricaoController(AdesaoPrescricaoService adesaoPrescricaoService) {
		this.adesaoPrescricaoService = adesaoPrescricaoService;
	}

	@PostMapping
	public ResponseEntity<AdesaoPrescricaoResponse> criar(@Valid @RequestBody AdesaoPrescricaoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(adesaoPrescricaoService.criar(request));
	}

	@GetMapping
	public Page<AdesaoPrescricaoResponse> listar(
			@RequestParam(required = false) Long prescricaoId,
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long responsavelId,
			@RequestParam(required = false) String tomou,
			Pageable pageable
	) {
		return adesaoPrescricaoService.listar(prescricaoId, animalId, responsavelId, tomou, pageable);
	}

	@GetMapping("/{id}")
	public AdesaoPrescricaoResponse buscarPorId(@PathVariable Long id) {
		return adesaoPrescricaoService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public AdesaoPrescricaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody AdesaoPrescricaoRequest request) {
		return adesaoPrescricaoService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		adesaoPrescricaoService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
