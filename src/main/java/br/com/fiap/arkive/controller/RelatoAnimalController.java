package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.CriarRelatoAnimalRequest;
import br.com.fiap.arkive.dto.response.RelatoAnimalResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.RelatoAnimalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/animais/{animalId}/relatos")
@Profile("!local-nodb")
@Validated
@Tag(name = "Relatos do animal", description = "Historico permanente de relatos enviados por responsaveis.")
public class RelatoAnimalController {

	private final RelatoAnimalService relatoAnimalService;

	public RelatoAnimalController(RelatoAnimalService relatoAnimalService) {
		this.relatoAnimalService = relatoAnimalService;
	}

	@PostMapping
	@Operation(summary = "Cria relato permanente", description = "Somente responsavel ativo e atualmente vinculado ao animal. O relato nao altera a observacao pendente.")
	public ResponseEntity<RelatoAnimalResponse> criar(
			@PathVariable Long animalId,
			@Valid @RequestBody CriarRelatoAnimalRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(relatoAnimalService.criar(animalId, request, principal));
	}

	@GetMapping
	@Operation(summary = "Lista e pesquisa relatos", description = "Busca sem diferenca de caixa ou acentos, com ordem fixa do mais novo para o mais antigo.")
	public Page<RelatoAnimalResponse> listar(
			@PathVariable Long animalId,
			@RequestParam(required = false)
			@Size(max = 100, message = "Busca deve ter no maximo 100 caracteres.") String busca,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "Pagina deve ser maior ou igual a zero.") int page,
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "Tamanho da pagina deve ser no minimo 1.")
			@Max(value = 50, message = "Tamanho da pagina deve ser no maximo 50.") int size,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return relatoAnimalService.listar(animalId, busca, page, size, principal);
	}
}
