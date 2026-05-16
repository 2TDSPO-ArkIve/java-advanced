package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AvaliacaoBemEstarRequest;
import br.com.fiap.arkive.dto.response.AvaliacaoBemEstarResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AvaliacaoBemEstar;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.AvaliacaoBemEstarRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
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
public class AvaliacaoBemEstarService {

	private static final Set<String> APETITES = Set.of("SEM APETITE", "REDUZIDO", "NORMAL", "AUMENTADO");
	private static final Set<String> ATIVIDADES = Set.of("BAIXA", "NORMAL", "ALTA");
	private static final Set<String> COMPORTAMENTOS = Set.of("APATICO", "NORMAL", "ALTERADO", "ANSIOSO", "AGRESSIVO");

	private final AvaliacaoBemEstarRepository avaliacaoRepository;
	private final AnimalRepository animalRepository;
	private final ResponsavelRepository responsavelRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final ConsultaService consultaService;
	private final EventoJornadaService eventoJornadaService;

	public AvaliacaoBemEstarService(
			AvaliacaoBemEstarRepository avaliacaoRepository,
			AnimalRepository animalRepository,
			ResponsavelRepository responsavelRepository,
			VeterinarioRepository veterinarioRepository,
			ConsultaService consultaService,
			EventoJornadaService eventoJornadaService
	) {
		this.avaliacaoRepository = avaliacaoRepository;
		this.animalRepository = animalRepository;
		this.responsavelRepository = responsavelRepository;
		this.veterinarioRepository = veterinarioRepository;
		this.consultaService = consultaService;
		this.eventoJornadaService = eventoJornadaService;
	}

	@Transactional
	public AvaliacaoBemEstarResponse criar(AvaliacaoBemEstarRequest request) {
		AvaliacaoBemEstar avaliacao = new AvaliacaoBemEstar();
		aplicarDados(avaliacao, request, true);
		AvaliacaoBemEstar salva = avaliacaoRepository.save(avaliacao);
		Long responsavelId = salva.getResponsavel() == null ? null : salva.getResponsavel().getId();
		Long veterinarioId = salva.getVeterinario() == null ? null : salva.getVeterinario().getId();
		eventoJornadaService.registrarEvento(
				"AVALIACAO_BEM_ESTAR_REGISTRADA",
				veterinarioId == null ? "RESPONSAVEL" : "VETERINARIO",
				responsavelId,
				veterinarioId,
				salva.getAnimal().getId(),
				null,
				"Avaliacao de bem-estar registrada.",
				eventoJornadaService.criarPayload("AvaliacaoBemEstar", salva.getId(), "AVALIACAO_BEM_ESTAR_REGISTRADA")
		);
		return AvaliacaoBemEstarResponse.fromEntity(salva);
	}

	@Transactional(readOnly = true)
	public Page<AvaliacaoBemEstarResponse> listar(
			Long animalId,
			Long responsavelId,
			Long veterinarioId,
			Long consultaId,
			String apetite,
			String atividade,
			String comportamento,
			Pageable pageable
	) {
		validarApetiteQuandoInformado(apetite);
		validarAtividadeQuandoInformada(atividade);
		validarComportamentoQuandoInformado(comportamento);
		return avaliacaoRepository.buscar(
				animalId,
				responsavelId,
				veterinarioId,
				consultaId,
				vazioParaNulo(apetite),
				vazioParaNulo(atividade),
				vazioParaNulo(comportamento),
				pageable
		).map(AvaliacaoBemEstarResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public AvaliacaoBemEstarResponse buscarPorId(Long id) {
		return AvaliacaoBemEstarResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public AvaliacaoBemEstarResponse atualizar(Long id, AvaliacaoBemEstarRequest request) {
		AvaliacaoBemEstar avaliacao = buscarEntidade(id);
		aplicarDados(avaliacao, request, false);
		return AvaliacaoBemEstarResponse.fromEntity(avaliacaoRepository.save(avaliacao));
	}

	@Transactional
	public void excluir(Long id) {
		AvaliacaoBemEstar avaliacao = buscarEntidade(id);
		try {
			avaliacaoRepository.delete(avaliacao);
			avaliacaoRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Avaliacao de bem-estar nao pode ser excluida porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	private AvaliacaoBemEstar buscarEntidade(Long id) {
		return avaliacaoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Avaliacao de bem-estar nao encontrada."));
	}

	private void aplicarDados(AvaliacaoBemEstar avaliacao, AvaliacaoBemEstarRequest request, boolean criando) {
		if (request.responsavelId() == null && request.veterinarioId() == null && request.consultaId() == null) {
			throw new BusinessException("Informe responsavel, veterinario ou consulta para a avaliacao.");
		}
		validarApetiteQuandoInformado(request.apetite());
		validarAtividadeQuandoInformada(request.atividade());
		validarComportamentoQuandoInformado(request.comportamento());
		LocalDateTime dataAvaliacao = criando && request.dataAvaliacao() == null ? LocalDateTime.now() : request.dataAvaliacao();
		Animal animal = buscarAnimal(request.animalId());
		Responsavel responsavel = request.responsavelId() == null ? null : buscarResponsavel(request.responsavelId());
		Veterinario veterinario = request.veterinarioId() == null ? null : buscarVeterinario(request.veterinarioId());
		Consulta consulta = request.consultaId() == null ? null : consultaService.buscarEntidade(request.consultaId());
		avaliacao.setAnimal(animal);
		avaliacao.setResponsavel(responsavel);
		avaliacao.setVeterinario(veterinario);
		avaliacao.setConsulta(consulta);
		avaliacao.setDataAvaliacao(dataAvaliacao == null ? avaliacao.getDataAvaliacao() : dataAvaliacao);
		avaliacao.setPeso(request.peso());
		avaliacao.setIdade(request.idade());
		avaliacao.setApetite(request.apetite());
		avaliacao.setAtividade(request.atividade());
		avaliacao.setComportamento(request.comportamento());
		avaliacao.setObservacao(request.observacao());
	}

	private Animal buscarAnimal(Long id) {
		return animalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Responsavel buscarResponsavel(Long id) {
		return responsavelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
	}

	private Veterinario buscarVeterinario(Long id) {
		return veterinarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Veterinario nao encontrado."));
	}

	private void validarApetiteQuandoInformado(String valor) {
		if (valor != null && !valor.isBlank() && !APETITES.contains(valor)) {
			throw new BusinessException("Apetite invalido.");
		}
	}

	private void validarAtividadeQuandoInformada(String valor) {
		if (valor != null && !valor.isBlank() && !ATIVIDADES.contains(valor)) {
			throw new BusinessException("Atividade invalida.");
		}
	}

	private void validarComportamentoQuandoInformado(String valor) {
		if (valor != null && !valor.isBlank() && !COMPORTAMENTOS.contains(valor)) {
			throw new BusinessException("Comportamento invalido.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
