package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AdesaoPrescricaoRequest;
import br.com.fiap.arkive.dto.response.AdesaoPrescricaoResponse;
import br.com.fiap.arkive.entity.AdesaoPrescricao;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AdesaoPrescricaoRepository;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Profile("!local-nodb")
public class AdesaoPrescricaoService {

	private final AdesaoPrescricaoRepository adesaoPrescricaoRepository;
	private final PrescricaoService prescricaoService;
	private final AnimalRepository animalRepository;
	private final ResponsavelRepository responsavelRepository;
	private final EventoJornadaService eventoJornadaService;

	public AdesaoPrescricaoService(
			AdesaoPrescricaoRepository adesaoPrescricaoRepository,
			PrescricaoService prescricaoService,
			AnimalRepository animalRepository,
			ResponsavelRepository responsavelRepository,
			EventoJornadaService eventoJornadaService
	) {
		this.adesaoPrescricaoRepository = adesaoPrescricaoRepository;
		this.prescricaoService = prescricaoService;
		this.animalRepository = animalRepository;
		this.responsavelRepository = responsavelRepository;
		this.eventoJornadaService = eventoJornadaService;
	}

	@Transactional
	public AdesaoPrescricaoResponse criar(AdesaoPrescricaoRequest request) {
		AdesaoPrescricao adesao = new AdesaoPrescricao();
		aplicarDados(adesao, request, true);
		AdesaoPrescricao salva = adesaoPrescricaoRepository.save(adesao);
		Long responsavelId = salva.getResponsavel() == null ? null : salva.getResponsavel().getId();
		eventoJornadaService.registrarEvento(
				"ADESAO_REGISTRADA",
				responsavelId == null ? "SISTEMA" : "RESPONSAVEL",
				responsavelId,
				null,
				salva.getAnimal().getId(),
				null,
				"Adesao de prescricao registrada.",
				eventoJornadaService.criarPayload("AdesaoPrescricao", salva.getId(), "ADESAO_REGISTRADA")
		);
		return AdesaoPrescricaoResponse.fromEntity(salva);
	}

	@Transactional(readOnly = true)
	public Page<AdesaoPrescricaoResponse> listar(Long prescricaoId, Long animalId, Long responsavelId, String tomou, Pageable pageable) {
		validarTomouQuandoInformado(tomou);
		return adesaoPrescricaoRepository.buscar(prescricaoId, animalId, responsavelId, vazioParaNulo(tomou), pageable)
				.map(AdesaoPrescricaoResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public AdesaoPrescricaoResponse buscarPorId(Long id) {
		return AdesaoPrescricaoResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public AdesaoPrescricaoResponse atualizar(Long id, AdesaoPrescricaoRequest request) {
		AdesaoPrescricao adesao = buscarEntidade(id);
		aplicarDados(adesao, request, false);
		return AdesaoPrescricaoResponse.fromEntity(adesaoPrescricaoRepository.save(adesao));
	}

	@Transactional
	public void excluir(Long id) {
		AdesaoPrescricao adesao = buscarEntidade(id);
		try {
			adesaoPrescricaoRepository.delete(adesao);
			adesaoPrescricaoRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Adesao de prescricao nao pode ser excluida porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	private AdesaoPrescricao buscarEntidade(Long id) {
		return adesaoPrescricaoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Adesao de prescricao nao encontrada."));
	}

	private void aplicarDados(AdesaoPrescricao adesao, AdesaoPrescricaoRequest request, boolean criando) {
		LocalDateTime dataRegistro = criando && request.dataRegistro() == null ? LocalDateTime.now() : request.dataRegistro();
		validarTomouObrigatorio(request.tomou());
		Prescricao prescricao = prescricaoService.buscarEntidade(request.prescricaoId());
		Animal animal = buscarAnimal(request.animalId());
		Responsavel responsavel = request.responsavelId() == null ? null : buscarResponsavel(request.responsavelId());
		if (!prescricao.getConsulta().getAnimal().getId().equals(animal.getId())) {
			throw new BusinessException("Animal da adesao deve ser o mesmo animal da consulta da prescricao.");
		}
		adesao.setPrescricao(prescricao);
		adesao.setAnimal(animal);
		adesao.setResponsavel(responsavel);
		adesao.setDataRegistro(dataRegistro == null ? adesao.getDataRegistro() : dataRegistro);
		adesao.setTomou(request.tomou());
		adesao.setObservacao(request.observacao());
	}

	private Animal buscarAnimal(Long id) {
		return animalRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Responsavel buscarResponsavel(Long id) {
		return responsavelRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
	}

	private void validarTomouObrigatorio(String tomou) {
		if (!"S".equals(tomou) && !"N".equals(tomou)) {
			throw new BusinessException("Tomou deve ser S ou N.");
		}
	}

	private void validarTomouQuandoInformado(String tomou) {
		if (tomou != null && !tomou.isBlank() && !"S".equals(tomou) && !"N".equals(tomou)) {
			throw new BusinessException("Tomou deve ser S ou N.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
