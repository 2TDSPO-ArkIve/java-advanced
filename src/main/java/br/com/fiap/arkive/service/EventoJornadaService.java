package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.EventoJornadaRequest;
import br.com.fiap.arkive.dto.response.EventoJornadaResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.EventoJornada;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.EventoJornadaRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@Profile("!local-nodb")
public class EventoJornadaService {

	private static final Set<String> ORIGENS = Set.of("APP", "WEB", "WHATSAPP", "API", "IA", "SISTEMA");
	private static final Set<String> ATORES = Set.of("RESPONSAVEL", "VETERINARIO", "CLINICA", "SISTEMA", "IA");
	private static final String ORIGEM_PADRAO = "API";
	private static final String ATOR_PADRAO = "SISTEMA";
	private static final String CANAL_PADRAO = "API";

	private final EventoJornadaRepository eventoJornadaRepository;
	private final ResponsavelRepository responsavelRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final AnimalRepository animalRepository;
	private final ClinicaRepository clinicaRepository;
	private final ObjectMapper objectMapper;

	public EventoJornadaService(
			EventoJornadaRepository eventoJornadaRepository,
			ResponsavelRepository responsavelRepository,
			VeterinarioRepository veterinarioRepository,
			AnimalRepository animalRepository,
			ClinicaRepository clinicaRepository,
			ObjectMapper objectMapper
	) {
		this.eventoJornadaRepository = eventoJornadaRepository;
		this.responsavelRepository = responsavelRepository;
		this.veterinarioRepository = veterinarioRepository;
		this.animalRepository = animalRepository;
		this.clinicaRepository = clinicaRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public EventoJornadaResponse criar(EventoJornadaRequest request) {
		EventoJornada evento = new EventoJornada();
		aplicarDados(evento, request);
		return EventoJornadaResponse.fromEntity(eventoJornadaRepository.save(evento));
	}

	@Transactional(readOnly = true)
	public Page<EventoJornadaResponse> listar(
			String tipoEvento,
			String origem,
			String ator,
			Long responsavelId,
			Long veterinarioId,
			Long animalId,
			Long clinicaId,
			String canal,
			Pageable pageable
	) {
		validarOrigemQuandoInformada(origem);
		validarAtorQuandoInformado(ator);
		return eventoJornadaRepository.buscar(
				vazioParaNulo(tipoEvento),
				vazioParaNulo(origem),
				vazioParaNulo(ator),
				responsavelId,
				veterinarioId,
				animalId,
				clinicaId,
				vazioParaNulo(canal),
				pageable
		).map(EventoJornadaResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public EventoJornadaResponse buscarPorId(Long id) {
		return EventoJornadaResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public Page<EventoJornadaResponse> timelinePorAnimal(Long animalId, Pageable pageable) {
		buscarAnimal(animalId);
		return eventoJornadaRepository.findByAnimalId(animalId, pageable).map(EventoJornadaResponse::fromEntity);
	}

	@Transactional
	public void registrarEvento(
			String tipoEvento,
			String ator,
			Long responsavelId,
			Long veterinarioId,
			Long animalId,
			Long clinicaId,
			String contexto,
			String payloadJson
	) {
		EventoJornada evento = new EventoJornada();
		evento.setTipoEvento(tipoEvento);
		evento.setDataEvento(LocalDateTime.now());
		evento.setOrigem(ORIGEM_PADRAO);
		evento.setAtor(ator == null ? ATOR_PADRAO : ator);
		evento.setResponsavel(responsavelId == null ? null : buscarResponsavel(responsavelId));
		evento.setVeterinario(veterinarioId == null ? null : buscarVeterinario(veterinarioId));
		evento.setAnimal(animalId == null ? null : buscarAnimal(animalId));
		evento.setClinica(clinicaId == null ? null : buscarClinica(clinicaId));
		evento.setCanal(CANAL_PADRAO);
		evento.setContexto(contexto);
		validarPayloadJson(payloadJson);
		evento.setPayloadJson(payloadJson);
		validarObrigatorios(evento);
		eventoJornadaRepository.save(evento);
	}

	public String criarPayload(String entidade, Long id, String acao) {
		try {
			return objectMapper.writeValueAsString(new EventoPayload(entidade, id, acao));
		} catch (JsonProcessingException ex) {
			throw new BusinessException("Nao foi possivel gerar payload do evento.");
		}
	}

	private EventoJornada buscarEntidade(Long id) {
		return eventoJornadaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Evento de jornada nao encontrado."));
	}

	private void aplicarDados(EventoJornada evento, EventoJornadaRequest request) {
		evento.setTipoEvento(request.tipoEvento());
		evento.setDataEvento(LocalDateTime.now());
		evento.setOrigem(request.origem());
		evento.setAtor(request.ator());
		evento.setResponsavel(request.responsavelId() == null ? null : buscarResponsavel(request.responsavelId()));
		evento.setVeterinario(request.veterinarioId() == null ? null : buscarVeterinario(request.veterinarioId()));
		evento.setAnimal(request.animalId() == null ? null : buscarAnimal(request.animalId()));
		evento.setClinica(request.clinicaId() == null ? null : buscarClinica(request.clinicaId()));
		evento.setCanal(request.canal());
		evento.setContexto(request.contexto());
		validarPayloadJson(request.payloadJson());
		evento.setPayloadJson(request.payloadJson());
		validarObrigatorios(evento);
	}

	private void validarObrigatorios(EventoJornada evento) {
		validarOrigemObrigatoria(evento.getOrigem());
		validarAtorQuandoInformado(evento.getAtor());
		if (evento.getTipoEvento() == null || evento.getTipoEvento().isBlank()) {
			throw new BusinessException("Tipo do evento deve ser informado.");
		}
	}

	private void validarOrigemObrigatoria(String origem) {
		if (!ORIGENS.contains(origem)) {
			throw new BusinessException("Origem deve ser APP, WEB, WHATSAPP, API, IA ou SISTEMA.");
		}
	}

	private void validarOrigemQuandoInformada(String origem) {
		if (origem != null && !origem.isBlank() && !ORIGENS.contains(origem)) {
			throw new BusinessException("Origem deve ser APP, WEB, WHATSAPP, API, IA ou SISTEMA.");
		}
	}

	private void validarAtorQuandoInformado(String ator) {
		if (ator != null && !ator.isBlank() && !ATORES.contains(ator)) {
			throw new BusinessException("Ator deve ser RESPONSAVEL, VETERINARIO, CLINICA, SISTEMA ou IA.");
		}
	}

	private void validarPayloadJson(String payloadJson) {
		if (payloadJson == null || payloadJson.isBlank()) {
			return;
		}
		try {
			objectMapper.readTree(payloadJson);
		} catch (JsonProcessingException ex) {
			throw new BusinessException("Payload JSON invalido.");
		}
	}

	private Responsavel buscarResponsavel(Long id) {
		return responsavelRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
	}

	private Veterinario buscarVeterinario(Long id) {
		return veterinarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Veterinario nao encontrado."));
	}

	private Animal buscarAnimal(Long id) {
		return animalRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Clinica buscarClinica(Long id) {
		return clinicaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada."));
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

	private record EventoPayload(String entity, Long id, String action) {
	}

}
