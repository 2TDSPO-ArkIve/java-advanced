package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.ConsultaService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/consultas")
@Profile("!local-nodb")
public class ConsultaController {

	private final ConsultaService consultaService;

	public ConsultaController(ConsultaService consultaService) {
		this.consultaService = consultaService;
	}

	@PostMapping
	public ResponseEntity<ConsultaResponse> criar(
			@Valid @RequestBody ConsultaRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.criar(request, principal));
	}

	@GetMapping
	public Page<ConsultaResponse> listar(
			@Parameter(description = "ID numerico exato da consulta.")
			@RequestParam(required = false) Long consultaId,
			@Parameter(description = "Trecho do nome do paciente, sem diferenciar maiusculas e minusculas.")
			@RequestParam(required = false) String pacienteNome,
			@Parameter(description = "ArkIve ID exato do paciente, sem diferenciar maiusculas e minusculas.")
			@RequestParam(required = false) String arkivePatientId,
			@Parameter(description = "Limite inicial inclusivo de data e hora em ISO-8601.", example = "2026-10-01T00:00:00")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHoraInicio,
			@Parameter(description = "Limite final inclusivo de data e hora em ISO-8601.", example = "2026-10-31T23:59:59")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHoraFim,
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long veterinarioId,
			@RequestParam(required = false) Long clinicaId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String modalidade,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return consultaService.listarAutorizado(
				consultaId, pacienteNome, arkivePatientId, dataHoraInicio, dataHoraFim,
				animalId, veterinarioId, clinicaId, status, modalidade, pageable, principal);
	}

	@GetMapping("/{id}")
	public ConsultaResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return consultaService.buscarPorIdAutorizado(id, principal);
	}

	@PutMapping("/{id}")
	public ConsultaResponse atualizar(
			@PathVariable Long id,
			@Valid @RequestBody ConsultaRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return consultaService.atualizar(id, request, principal);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		consultaService.excluir(id, principal);
		return ResponseEntity.noContent().build();
	}

}
