package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Profile("!local-nodb")
public class ConsultaService {

	private static final Set<String> MODALIDADES = Set.of("PRESENCIAL", "REMOTA");
	private static final Set<String> STATUS = Set.of("AG", "EP", "AP", "FI", "CA");

	private final ConsultaRepository consultaRepository;
	private final AnimalRepository animalRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final ClinicaRepository clinicaRepository;

	public ConsultaService(
			ConsultaRepository consultaRepository,
			AnimalRepository animalRepository,
			VeterinarioRepository veterinarioRepository,
			ClinicaRepository clinicaRepository
	) {
		this.consultaRepository = consultaRepository;
		this.animalRepository = animalRepository;
		this.veterinarioRepository = veterinarioRepository;
		this.clinicaRepository = clinicaRepository;
	}

	@Transactional
	public ConsultaResponse criar(ConsultaRequest request) {
		Consulta consulta = new Consulta();
		aplicarDados(consulta, request, true);
		return ConsultaResponse.fromEntity(consultaRepository.save(consulta));
	}

	@Transactional(readOnly = true)
	public Page<ConsultaResponse> listar(Long animalId, Long veterinarioId, Long clinicaId, String status, String modalidade, Pageable pageable) {
		validarStatusQuandoInformado(status);
		validarModalidadeQuandoInformada(modalidade);
		return consultaRepository.buscar(animalId, veterinarioId, clinicaId, vazioParaNulo(status), vazioParaNulo(modalidade), pageable)
				.map(ConsultaResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public ConsultaResponse buscarPorId(Long id) {
		return ConsultaResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public ConsultaResponse atualizar(Long id, ConsultaRequest request) {
		Consulta consulta = buscarEntidade(id);
		aplicarDados(consulta, request, false);
		return ConsultaResponse.fromEntity(consultaRepository.save(consulta));
	}

	@Transactional
	public void excluir(Long id) {
		Consulta consulta = buscarEntidade(id);
		try {
			consultaRepository.delete(consulta);
			consultaRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Consulta nao pode ser excluida porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	@Transactional(readOnly = true)
	public Consulta buscarEntidade(Long id) {
		return consultaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Consulta nao encontrada."));
	}

	private void aplicarDados(Consulta consulta, ConsultaRequest request, boolean criando) {
		String status = criando && request.status() == null ? "AG" : request.status();
		validarModalidadeObrigatoria(request.modalidade());
		validarStatusQuandoInformado(status);
		Animal animal = buscarAnimal(request.animalId());
		Veterinario veterinario = buscarVeterinario(request.veterinarioId());
		Clinica clinica = request.clinicaId() == null ? null : buscarClinica(request.clinicaId());
		consulta.setDataHora(request.dataHora());
		consulta.setModalidade(request.modalidade());
		consulta.setMotivo(request.motivo());
		consulta.setSintomas(request.sintomas());
		consulta.setObservacao(request.observacao());
		consulta.setPeso(request.peso());
		consulta.setTranscricao(request.transcricao());
		consulta.setStatus(status == null ? consulta.getStatus() : status);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setClinica(clinica);
	}

	private Animal buscarAnimal(Long id) {
		return animalRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Veterinario buscarVeterinario(Long id) {
		return veterinarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Veterinario nao encontrado."));
	}

	private Clinica buscarClinica(Long id) {
		return clinicaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada."));
	}

	private void validarModalidadeObrigatoria(String modalidade) {
		if (!MODALIDADES.contains(modalidade)) {
			throw new BusinessException("Modalidade deve ser PRESENCIAL ou REMOTA.");
		}
	}

	private void validarModalidadeQuandoInformada(String modalidade) {
		if (modalidade != null && !modalidade.isBlank() && !MODALIDADES.contains(modalidade)) {
			throw new BusinessException("Modalidade deve ser PRESENCIAL ou REMOTA.");
		}
	}

	private void validarStatusQuandoInformado(String status) {
		if (status != null && !status.isBlank() && !STATUS.contains(status)) {
			throw new BusinessException("Status deve ser AG, EP, AP, FI ou CA.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
