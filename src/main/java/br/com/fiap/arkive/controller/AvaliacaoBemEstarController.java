package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.AvaliacaoBemEstarRequest;
import br.com.fiap.arkive.dto.response.AvaliacaoBemEstarResponse;
import br.com.fiap.arkive.service.AvaliacaoBemEstarService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/avaliacoes-bem-estar")
@Profile("!local-nodb")
public class AvaliacaoBemEstarController {

	private final AvaliacaoBemEstarService avaliacaoService;

	public AvaliacaoBemEstarController(AvaliacaoBemEstarService avaliacaoService) {
		this.avaliacaoService = avaliacaoService;
	}

	@PostMapping
	public ResponseEntity<AvaliacaoBemEstarResponse> criar(@Valid @RequestBody AvaliacaoBemEstarRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResponseEntity.status(HttpStatus.CREATED).body(avaliacaoService.criar(request, principal));
	}

	@GetMapping
	public Page<AvaliacaoBemEstarResponse> listar(
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long responsavelId,
			@RequestParam(required = false) Long veterinarioId,
			@RequestParam(required = false) Long consultaId,
			@RequestParam(required = false) String apetite,
			@RequestParam(required = false) String atividade,
			@RequestParam(required = false) String comportamento,
			Pageable pageable, @AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return avaliacaoService.listar(animalId, responsavelId, veterinarioId, consultaId, apetite, atividade, comportamento, pageable, principal);
	}

	@GetMapping("/{id}")
	public AvaliacaoBemEstarResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return avaliacaoService.buscarPorId(id, principal);
	}

	@PutMapping("/{id}")
	public AvaliacaoBemEstarResponse atualizar(@PathVariable Long id, @Valid @RequestBody AvaliacaoBemEstarRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return avaliacaoService.atualizar(id, request, principal);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		avaliacaoService.excluir(id, principal);
		return ResponseEntity.noContent().build();
	}

}
