package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.EventoPreventivoRequest;
import br.com.fiap.arkive.dto.response.EventoPreventivoResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.EventoPreventivo;
import br.com.fiap.arkive.entity.ProtocoloPreventivo;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.EventoPreventivoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@Profile("!local-nodb")
public class EventoPreventivoService {

	private static final Set<String> STATUS = Set.of("REALIZADO", "PENDENTE", "ATRASADO");

	private final EventoPreventivoRepository eventoRepository;
	private final AnimalRepository animalRepository;
	private final ProtocoloPreventivoService protocoloService;
	private final ConsultaService consultaService;

	public EventoPreventivoService(
			EventoPreventivoRepository eventoRepository,
			AnimalRepository animalRepository,
			ProtocoloPreventivoService protocoloService,
			ConsultaService consultaService
	) {
		this.eventoRepository = eventoRepository;
		this.animalRepository = animalRepository;
		this.protocoloService = protocoloService;
		this.consultaService = consultaService;
	}

	@Transactional
	public EventoPreventivoResponse criar(EventoPreventivoRequest request) {
		EventoPreventivo evento = new EventoPreventivo();
		aplicarDados(evento, request, true);
		return EventoPreventivoResponse.fromEntity(eventoRepository.save(evento));
	}

	@Transactional(readOnly = true)
	public Page<EventoPreventivoResponse> listar(Long animalId, Long protocoloId, Long consultaId, String status, String alerta, Pageable pageable) {
		validarStatusQuandoInformado(status);
		validarSNQuandoInformado(alerta, "Alerta");
		return eventoRepository.buscar(animalId, protocoloId, consultaId, vazioParaNulo(status), vazioParaNulo(alerta), pageable)
				.map(EventoPreventivoResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public EventoPreventivoResponse buscarPorId(Long id) {
		return EventoPreventivoResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public EventoPreventivoResponse atualizar(Long id, EventoPreventivoRequest request) {
		EventoPreventivo evento = buscarEntidade(id);
		aplicarDados(evento, request, false);
		return EventoPreventivoResponse.fromEntity(eventoRepository.save(evento));
	}

	@Transactional
	public void excluir(Long id) {
		EventoPreventivo evento = buscarEntidade(id);
		try {
			eventoRepository.delete(evento);
			eventoRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Evento preventivo nao pode ser excluido porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	@Transactional(readOnly = true)
	public EventoPreventivo buscarEntidade(Long id) {
		return eventoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Evento preventivo nao encontrado."));
	}

	private void aplicarDados(EventoPreventivo evento, EventoPreventivoRequest request, boolean criando) {
		String status = criando && request.status() == null ? "PENDENTE" : request.status();
		String alerta = criando && request.alerta() == null ? "N" : request.alerta();
		validarStatusObrigatorio(status);
		validarSNQuandoInformado(alerta, "Alerta");
		validarDatas(status, request.dataAplicacao(), request.dataProximo());
		Animal animal = animalRepository.findById(request.animalId()).orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
		ProtocoloPreventivo protocolo = protocoloService.buscarEntidade(request.protocoloId());
		Consulta consulta = request.consultaId() == null ? null : consultaService.buscarEntidade(request.consultaId());
		evento.setDataAplicacao(request.dataAplicacao());
		evento.setDataProximo(request.dataProximo());
		evento.setStatus(status);
		evento.setAlerta(alerta == null ? evento.getAlerta() : alerta);
		evento.setObservacao(request.observacao());
		evento.setAnimal(animal);
		evento.setProtocolo(protocolo);
		evento.setConsulta(consulta);
	}

	private void validarDatas(String status, LocalDate dataAplicacao, LocalDate dataProximo) {
		if ("REALIZADO".equals(status) && dataAplicacao == null) {
			throw new BusinessException("Data aplicacao deve ser informada para status REALIZADO.");
		}
		if (("PENDENTE".equals(status) || "ATRASADO".equals(status)) && dataAplicacao != null) {
			throw new BusinessException("Data aplicacao deve ficar vazia para status PENDENTE ou ATRASADO.");
		}
		if (dataAplicacao != null && dataProximo.isBefore(dataAplicacao)) {
			throw new BusinessException("Data proximo deve ser maior ou igual a data aplicacao.");
		}
	}

	private void validarStatusObrigatorio(String status) {
		if (!STATUS.contains(status)) {
			throw new BusinessException("Status de evento preventivo invalido.");
		}
	}

	private void validarStatusQuandoInformado(String status) {
		if (status != null && !status.isBlank() && !STATUS.contains(status)) {
			throw new BusinessException("Status de evento preventivo invalido.");
		}
	}

	private void validarSNQuandoInformado(String valor, String campo) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException(campo + " deve ser S ou N.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
