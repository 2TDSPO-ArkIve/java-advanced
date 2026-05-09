package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.VeterinarioRequest;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.service.VeterinarioService;
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
@RequestMapping("/api/veterinarios")
@Profile("!local-nodb")
public class VeterinarioController {

	private final VeterinarioService veterinarioService;

	public VeterinarioController(VeterinarioService veterinarioService) {
		this.veterinarioService = veterinarioService;
	}

	@PostMapping
	public ResponseEntity<VeterinarioResponse> criar(@Valid @RequestBody VeterinarioRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(veterinarioService.criar(request));
	}

	@GetMapping
	public Page<VeterinarioResponse> listar(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) String crmv,
			@RequestParam(required = false) Long clinicaId,
			@RequestParam(required = false) String ativo,
			Pageable pageable
	) {
		return veterinarioService.listar(nome, crmv, clinicaId, ativo, pageable);
	}

	@GetMapping("/{id}")
	public VeterinarioResponse buscarPorId(@PathVariable Long id) {
		return veterinarioService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public VeterinarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody VeterinarioRequest request) {
		return veterinarioService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		veterinarioService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
