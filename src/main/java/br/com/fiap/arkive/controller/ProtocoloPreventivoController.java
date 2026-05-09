package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.ProtocoloPreventivoRequest;
import br.com.fiap.arkive.dto.response.ProtocoloPreventivoResponse;
import br.com.fiap.arkive.service.ProtocoloPreventivoService;
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
@RequestMapping("/api/protocolos-preventivos")
@Profile("!local-nodb")
public class ProtocoloPreventivoController {

	private final ProtocoloPreventivoService protocoloService;

	public ProtocoloPreventivoController(ProtocoloPreventivoService protocoloService) {
		this.protocoloService = protocoloService;
	}

	@PostMapping
	public ResponseEntity<ProtocoloPreventivoResponse> criar(@Valid @RequestBody ProtocoloPreventivoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(protocoloService.criar(request));
	}

	@GetMapping
	public Page<ProtocoloPreventivoResponse> listar(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) String tipo,
			@RequestParam(required = false) Long especieId,
			@RequestParam(required = false) Long racaId,
			@RequestParam(required = false) String ativo,
			Pageable pageable
	) {
		return protocoloService.listar(nome, tipo, especieId, racaId, ativo, pageable);
	}

	@GetMapping("/{id}")
	public ProtocoloPreventivoResponse buscarPorId(@PathVariable Long id) {
		return protocoloService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public ProtocoloPreventivoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProtocoloPreventivoRequest request) {
		return protocoloService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		protocoloService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
