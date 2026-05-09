package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.FeedbackNpsRequest;
import br.com.fiap.arkive.dto.response.FeedbackNpsResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.FeedbackNps;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.FeedbackNpsRepository;
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
public class FeedbackNpsService {

	private final FeedbackNpsRepository feedbackRepository;
	private final ResponsavelRepository responsavelRepository;
	private final AnimalRepository animalRepository;
	private final ClinicaRepository clinicaRepository;
	private final ConsultaService consultaService;

	public FeedbackNpsService(
			FeedbackNpsRepository feedbackRepository,
			ResponsavelRepository responsavelRepository,
			AnimalRepository animalRepository,
			ClinicaRepository clinicaRepository,
			ConsultaService consultaService
	) {
		this.feedbackRepository = feedbackRepository;
		this.responsavelRepository = responsavelRepository;
		this.animalRepository = animalRepository;
		this.clinicaRepository = clinicaRepository;
		this.consultaService = consultaService;
	}

	@Transactional
	public FeedbackNpsResponse criar(FeedbackNpsRequest request) {
		FeedbackNps feedback = new FeedbackNps();
		aplicarDados(feedback, request, true);
		return FeedbackNpsResponse.fromEntity(feedbackRepository.save(feedback));
	}

	@Transactional(readOnly = true)
	public Page<FeedbackNpsResponse> listar(Long responsavelId, Long animalId, Long clinicaId, Long consultaId, Integer nota, Pageable pageable) {
		return feedbackRepository.buscar(responsavelId, animalId, clinicaId, consultaId, nota, pageable)
				.map(FeedbackNpsResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public FeedbackNpsResponse buscarPorId(Long id) {
		return FeedbackNpsResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public FeedbackNpsResponse atualizar(Long id, FeedbackNpsRequest request) {
		FeedbackNps feedback = buscarEntidade(id);
		aplicarDados(feedback, request, false);
		return FeedbackNpsResponse.fromEntity(feedbackRepository.save(feedback));
	}

	@Transactional
	public void excluir(Long id) {
		FeedbackNps feedback = buscarEntidade(id);
		try {
			feedbackRepository.delete(feedback);
			feedbackRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Feedback NPS nao pode ser excluido porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	private FeedbackNps buscarEntidade(Long id) {
		return feedbackRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Feedback NPS nao encontrado."));
	}

	private void aplicarDados(FeedbackNps feedback, FeedbackNpsRequest request, boolean criando) {
		if (request.responsavelId() == null && request.animalId() == null && request.clinicaId() == null && request.consultaId() == null) {
			throw new BusinessException("Informe ao menos um contexto para o feedback NPS.");
		}
		LocalDateTime dataResposta = criando && request.dataResposta() == null ? LocalDateTime.now() : request.dataResposta();
		Responsavel responsavel = request.responsavelId() == null ? null : buscarResponsavel(request.responsavelId());
		Animal animal = request.animalId() == null ? null : buscarAnimal(request.animalId());
		Clinica clinica = request.clinicaId() == null ? null : buscarClinica(request.clinicaId());
		Consulta consulta = request.consultaId() == null ? null : consultaService.buscarEntidade(request.consultaId());
		feedback.setResponsavel(responsavel);
		feedback.setAnimal(animal);
		feedback.setClinica(clinica);
		feedback.setConsulta(consulta);
		feedback.setNota(request.nota());
		feedback.setComentario(request.comentario());
		feedback.setDataResposta(dataResposta == null ? feedback.getDataResposta() : dataResposta);
	}

	private Responsavel buscarResponsavel(Long id) {
		return responsavelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
	}

	private Animal buscarAnimal(Long id) {
		return animalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Clinica buscarClinica(Long id) {
		return clinicaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada."));
	}

}
