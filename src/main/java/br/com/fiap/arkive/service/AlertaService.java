package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AlertaRequest;
import br.com.fiap.arkive.dto.response.AlertaResponse;
import br.com.fiap.arkive.entity.Alerta;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.EventoPreventivo;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AlertaRepository;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@Profile("!local-nodb")
public class AlertaService {

	private static final Set<String> TIPOS = Set.of("VACINA", "RETORNO", "MEDICAMENTO", "CHECK-UP");
	private static final Set<String> STATUS = Set.of("ENVIADO", "LIDO", "IGNORADO");
	private static final Set<String> CANAIS = Set.of("APP", "WHATSAPP", "EMAIL");

	private final AlertaRepository alertaRepository;
	private final AnimalRepository animalRepository;
	private final ResponsavelRepository responsavelRepository;
	private final ClinicaRepository clinicaRepository;
	private final EventoPreventivoService eventoPreventivoService;
	private final EventoJornadaService eventoJornadaService;

	public AlertaService(
			AlertaRepository alertaRepository,
			AnimalRepository animalRepository,
			ResponsavelRepository responsavelRepository,
			ClinicaRepository clinicaRepository,
			EventoPreventivoService eventoPreventivoService,
			EventoJornadaService eventoJornadaService
	) {
		this.alertaRepository = alertaRepository;
		this.animalRepository = animalRepository;
		this.responsavelRepository = responsavelRepository;
		this.clinicaRepository = clinicaRepository;
		this.eventoPreventivoService = eventoPreventivoService;
		this.eventoJornadaService = eventoJornadaService;
	}

	@Transactional
	public AlertaResponse criar(AlertaRequest request) {
		Alerta alerta = new Alerta();
		aplicarDados(alerta, request, true);
		return AlertaResponse.fromEntity(alertaRepository.save(alerta));
	}

	@Transactional(readOnly = true)
	public Page<AlertaResponse> listar(Long animalId, Long responsavelId, Long clinicaId, Long eventoPreventivoId, String tipo, String status, String canal, Pageable pageable) {
		validarTipoQuandoInformado(tipo);
		validarStatusQuandoInformado(status);
		validarCanalQuandoInformado(canal);
		return alertaRepository.buscar(animalId, responsavelId, clinicaId, eventoPreventivoId, vazioParaNulo(tipo), vazioParaNulo(status), vazioParaNulo(canal), pageable)
				.map(AlertaResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public AlertaResponse buscarPorId(Long id) {
		return AlertaResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public AlertaResponse atualizar(Long id, AlertaRequest request) {
		Alerta alerta = buscarEntidade(id);
		aplicarDados(alerta, request, false);
		return AlertaResponse.fromEntity(alertaRepository.save(alerta));
	}

	@Transactional
	public AlertaResponse marcarComoLido(Long id) {
		Alerta alerta = buscarEntidade(id);
		alerta.setStatus("LIDO");
		alerta.setDataLeitura(LocalDateTime.now());
		Alerta salvo = alertaRepository.save(alerta);
		Long responsavelId = salvo.getResponsavel() == null ? null : salvo.getResponsavel().getId();
		Long clinicaId = salvo.getClinica() == null ? null : salvo.getClinica().getId();
		eventoJornadaService.registrarEvento(
				"ALERTA_LIDO",
				responsavelId == null ? "CLINICA" : "RESPONSAVEL",
				responsavelId,
				null,
				salvo.getAnimal().getId(),
				clinicaId,
				"Alerta marcado como lido.",
				eventoJornadaService.criarPayload("Alerta", salvo.getId(), "ALERTA_LIDO")
		);
		return AlertaResponse.fromEntity(salvo);
	}

	@Transactional
	public void excluir(Long id) {
		Alerta alerta = buscarEntidade(id);
		try {
			alertaRepository.delete(alerta);
			alertaRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Alerta nao pode ser excluido porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	private Alerta buscarEntidade(Long id) {
		return alertaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alerta nao encontrado."));
	}

	private void aplicarDados(Alerta alerta, AlertaRequest request, boolean criando) {
		if (request.responsavelId() == null && request.clinicaId() == null) {
			throw new BusinessException("Informe responsavel ou clinica como destino do alerta.");
		}
		String status = criando && request.status() == null ? "ENVIADO" : request.status();
		LocalDateTime dataEnvio = criando && request.dataEnvio() == null ? LocalDateTime.now() : request.dataEnvio();
		validarTipoObrigatorio(request.tipo());
		validarStatusQuandoInformado(status);
		validarCanalObrigatorio(request.canal());
		validarLeitura(dataEnvio == null ? alerta.getDataEnvio() : dataEnvio, request.dataLeitura());
		Animal animal = animalRepository.findById(request.animalId()).orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
		Responsavel responsavel = request.responsavelId() == null ? null : buscarResponsavel(request.responsavelId());
		Clinica clinica = request.clinicaId() == null ? null : buscarClinica(request.clinicaId());
		EventoPreventivo evento = request.eventoPreventivoId() == null ? null : eventoPreventivoService.buscarEntidade(request.eventoPreventivoId());
		alerta.setTipo(request.tipo());
		alerta.setMensagem(request.mensagem());
		alerta.setDataEnvio(dataEnvio == null ? alerta.getDataEnvio() : dataEnvio);
		alerta.setDataLeitura(request.dataLeitura());
		alerta.setStatus(status == null ? alerta.getStatus() : status);
		alerta.setCanal(request.canal());
		alerta.setAnimal(animal);
		alerta.setResponsavel(responsavel);
		alerta.setClinica(clinica);
		alerta.setEventoPreventivo(evento);
	}

	private Responsavel buscarResponsavel(Long id) {
		return responsavelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
	}

	private Clinica buscarClinica(Long id) {
		return clinicaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada."));
	}

	private void validarLeitura(LocalDateTime dataEnvio, LocalDateTime dataLeitura) {
		if (dataLeitura != null && dataLeitura.isBefore(dataEnvio)) {
			throw new BusinessException("Data leitura deve ser maior ou igual a data envio.");
		}
	}

	private void validarTipoObrigatorio(String tipo) {
		if (!TIPOS.contains(tipo)) {
			throw new BusinessException("Tipo de alerta invalido.");
		}
	}

	private void validarTipoQuandoInformado(String tipo) {
		if (tipo != null && !tipo.isBlank() && !TIPOS.contains(tipo)) {
			throw new BusinessException("Tipo de alerta invalido.");
		}
	}

	private void validarStatusQuandoInformado(String status) {
		if (status != null && !status.isBlank() && !STATUS.contains(status)) {
			throw new BusinessException("Status de alerta invalido.");
		}
	}

	private void validarCanalObrigatorio(String canal) {
		if (!CANAIS.contains(canal)) {
			throw new BusinessException("Canal de alerta invalido.");
		}
	}

	private void validarCanalQuandoInformado(String canal) {
		if (canal != null && !canal.isBlank() && !CANAIS.contains(canal)) {
			throw new BusinessException("Canal de alerta invalido.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
