package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.PrescricaoRequest;
import br.com.fiap.arkive.dto.response.PrescricaoResponse;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.PrescricaoRepository;
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
public class PrescricaoService {

	private static final Set<String> VIAS_ADMINISTRACAO = Set.of("Oral", "Injetavel", "Topico", "Ocular", "Otologico", "Outro");

	private final PrescricaoRepository prescricaoRepository;
	private final ConsultaService consultaService;

	public PrescricaoService(PrescricaoRepository prescricaoRepository, ConsultaService consultaService) {
		this.prescricaoRepository = prescricaoRepository;
		this.consultaService = consultaService;
	}

	@Transactional
	public PrescricaoResponse criar(PrescricaoRequest request) {
		Prescricao prescricao = new Prescricao();
		aplicarDados(prescricao, request);
		return PrescricaoResponse.fromEntity(prescricaoRepository.save(prescricao));
	}

	@Transactional(readOnly = true)
	public Page<PrescricaoResponse> listar(Long consultaId, String medicamento, Pageable pageable) {
		return prescricaoRepository.buscar(consultaId, vazioParaNulo(medicamento), pageable)
				.map(PrescricaoResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public PrescricaoResponse buscarPorId(Long id) {
		return PrescricaoResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public PrescricaoResponse atualizar(Long id, PrescricaoRequest request) {
		Prescricao prescricao = buscarEntidade(id);
		aplicarDados(prescricao, request);
		return PrescricaoResponse.fromEntity(prescricaoRepository.save(prescricao));
	}

	@Transactional
	public void excluir(Long id) {
		Prescricao prescricao = buscarEntidade(id);
		try {
			prescricaoRepository.delete(prescricao);
			prescricaoRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Prescricao nao pode ser excluida porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	@Transactional(readOnly = true)
	public Prescricao buscarEntidade(Long id) {
		return prescricaoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Prescricao nao encontrada."));
	}

	private void aplicarDados(Prescricao prescricao, PrescricaoRequest request) {
		validarViaQuandoInformada(request.viaAdministracao());
		validarDataFim(request.dataInicio(), request.dataFim());
		Consulta consulta = consultaService.buscarEntidade(request.consultaId());
		prescricao.setMedicamento(request.medicamento());
		prescricao.setDosagem(request.dosagem());
		prescricao.setFrequencia(request.frequencia());
		prescricao.setViaAdministracao(request.viaAdministracao());
		prescricao.setDataInicio(request.dataInicio());
		prescricao.setDataFim(request.dataFim());
		prescricao.setInstrucoes(request.instrucoes());
		prescricao.setConsulta(consulta);
	}

	private void validarViaQuandoInformada(String viaAdministracao) {
		if (viaAdministracao != null && !viaAdministracao.isBlank() && !VIAS_ADMINISTRACAO.contains(viaAdministracao)) {
			throw new BusinessException("Via de administracao invalida.");
		}
	}

	private void validarDataFim(LocalDate dataInicio, LocalDate dataFim) {
		if (dataFim != null && dataFim.isBefore(dataInicio)) {
			throw new BusinessException("Data fim nao pode ser anterior a data inicio.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
