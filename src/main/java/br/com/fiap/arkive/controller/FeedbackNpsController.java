package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.FeedbackNpsRequest;
import br.com.fiap.arkive.dto.response.FeedbackNpsResponse;
import br.com.fiap.arkive.service.FeedbackNpsService;
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
@RequestMapping("/api/feedbacks-nps")
@Profile("!local-nodb")
public class FeedbackNpsController {

	private final FeedbackNpsService feedbackService;

	public FeedbackNpsController(FeedbackNpsService feedbackService) {
		this.feedbackService = feedbackService;
	}

	@PostMapping
	public ResponseEntity<FeedbackNpsResponse> criar(@Valid @RequestBody FeedbackNpsRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.criar(request));
	}

	@GetMapping
	public Page<FeedbackNpsResponse> listar(
			@RequestParam(required = false) Long responsavelId,
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long clinicaId,
			@RequestParam(required = false) Long veterinarioId,
			@RequestParam(required = false) Long consultaId,
			@RequestParam(required = false) Integer nota,
			Pageable pageable
	) {
		return feedbackService.listar(responsavelId, animalId, clinicaId, veterinarioId, consultaId, nota, pageable);
	}

	@GetMapping("/{id}")
	public FeedbackNpsResponse buscarPorId(@PathVariable Long id) {
		return feedbackService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public FeedbackNpsResponse atualizar(@PathVariable Long id, @Valid @RequestBody FeedbackNpsRequest request) {
		return feedbackService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		feedbackService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
