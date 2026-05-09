package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.DiagnosticoRequest;
import br.com.fiap.arkive.dto.response.DiagnosticoResponse;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Doenca;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.DoencaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@Profile("!local-nodb")
public class DiagnosticoService {

	private static final Set<String> SEVERIDADES = Set.of("Leve", "Moderada", "Grave");

	private final DiagnosticoRepository diagnosticoRepository;
	private final ConsultaService consultaService;
	private final DoencaRepository doencaRepository;

	public DiagnosticoService(
			DiagnosticoRepository diagnosticoRepository,
			ConsultaService consultaService,
			DoencaRepository doencaRepository
	) {
		this.diagnosticoRepository = diagnosticoRepository;
		this.consultaService = consultaService;
		this.doencaRepository = doencaRepository;
	}

	@Transactional
	public DiagnosticoResponse criar(DiagnosticoRequest request) {
		Diagnostico diagnostico = new Diagnostico();
		aplicarDados(diagnostico, request, true);
		return DiagnosticoResponse.fromEntity(diagnosticoRepository.save(diagnostico));
	}

	@Transactional(readOnly = true)
	public Page<DiagnosticoResponse> listar(Long consultaId, Long doencaId, String severidade, String confirmado, Pageable pageable) {
		validarSeveridadeQuandoInformada(severidade);
		validarSNQuandoInformado(confirmado, "Confirmado");
		return diagnosticoRepository.buscar(consultaId, doencaId, vazioParaNulo(severidade), vazioParaNulo(confirmado), pageable)
				.map(DiagnosticoResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public DiagnosticoResponse buscarPorId(Long id) {
		return DiagnosticoResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public DiagnosticoResponse atualizar(Long id, DiagnosticoRequest request) {
		Diagnostico diagnostico = buscarEntidade(id);
		aplicarDados(diagnostico, request, false);
		return DiagnosticoResponse.fromEntity(diagnosticoRepository.save(diagnostico));
	}

	@Transactional
	public void excluir(Long id) {
		Diagnostico diagnostico = buscarEntidade(id);
		try {
			diagnosticoRepository.delete(diagnostico);
			diagnosticoRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Diagnostico nao pode ser excluido porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	private Diagnostico buscarEntidade(Long id) {
		return diagnosticoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Diagnostico nao encontrado."));
	}

	private void aplicarDados(Diagnostico diagnostico, DiagnosticoRequest request, boolean criando) {
		String confirmado = criando && request.confirmado() == null ? "S" : request.confirmado();
		validarSeveridadeQuandoInformada(request.severidade());
		validarSNQuandoInformado(confirmado, "Confirmado");
		validarSNQuandoInformado(request.validacaoVet(), "Validacao vet");
		validarConfianca(request.confianca());
		Consulta consulta = consultaService.buscarEntidade(request.consultaId());
		Doenca doenca = request.doencaId() == null ? null : buscarDoenca(request.doencaId());
		diagnostico.setDiagnostico(request.diagnostico());
		diagnostico.setSeveridade(request.severidade());
		diagnostico.setConfirmado(confirmado == null ? diagnostico.getConfirmado() : confirmado);
		diagnostico.setInsightIa(request.insightIa());
		diagnostico.setConfianca(request.confianca());
		diagnostico.setValidacaoVet(request.validacaoVet());
		diagnostico.setConsulta(consulta);
		diagnostico.setDoenca(doenca);
	}

	private Doenca buscarDoenca(Long id) {
		return doencaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Doenca nao encontrada."));
	}

	private void validarSeveridadeQuandoInformada(String severidade) {
		if (severidade != null && !severidade.isBlank() && !SEVERIDADES.contains(severidade)) {
			throw new BusinessException("Severidade deve ser Leve, Moderada ou Grave.");
		}
	}

	private void validarSNQuandoInformado(String valor, String campo) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException(campo + " deve ser S ou N.");
		}
	}

	private void validarConfianca(BigDecimal confianca) {
		if (confianca != null && (confianca.compareTo(BigDecimal.ZERO) < 0 || confianca.compareTo(BigDecimal.valueOf(100)) > 0)) {
			throw new BusinessException("Confianca deve estar entre 0 e 100.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
