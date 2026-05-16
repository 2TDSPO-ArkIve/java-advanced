package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.EventoJornadaRequest;
import br.com.fiap.arkive.dto.response.EventoJornadaResponse;
import br.com.fiap.arkive.service.EventoJornadaService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eventos-jornada")
@Profile("!local-nodb")
public class EventoJornadaController {

	private final EventoJornadaService eventoJornadaService;

	public EventoJornadaController(EventoJornadaService eventoJornadaService) {
		this.eventoJornadaService = eventoJornadaService;
	}

	@PostMapping
	public ResponseEntity<EventoJornadaResponse> criar(@Valid @RequestBody EventoJornadaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(eventoJornadaService.criar(request));
	}

	@GetMapping
	public Page<EventoJornadaResponse> listar(
			@RequestParam(required = false) String tipoEvento,
			@RequestParam(required = false) String origem,
			@RequestParam(required = false) String ator,
			@RequestParam(required = false) Long responsavelId,
			@RequestParam(required = false) Long veterinarioId,
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long clinicaId,
			@RequestParam(required = false) String canal,
			Pageable pageable
	) {
		return eventoJornadaService.listar(
				tipoEvento,
				origem,
				ator,
				responsavelId,
				veterinarioId,
				animalId,
				clinicaId,
				canal,
				pageable
		);
	}

	@GetMapping("/{id}")
	public EventoJornadaResponse buscarPorId(@PathVariable Long id) {
		return eventoJornadaService.buscarPorId(id);
	}

	@GetMapping("/animal/{animalId}/timeline")
	public Page<EventoJornadaResponse> timelinePorAnimal(@PathVariable Long animalId, Pageable pageable) {
		return eventoJornadaService.timelinePorAnimal(animalId, pageable);
	}

}
