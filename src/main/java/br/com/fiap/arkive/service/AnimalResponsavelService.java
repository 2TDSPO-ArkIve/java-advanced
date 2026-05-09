package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AnimalResponsavelRequest;
import br.com.fiap.arkive.dto.response.AnimalResponsavelResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.AnimalResponsavelId;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Profile("!local-nodb")
public class AnimalResponsavelService {

	private static final Set<String> TIPOS_VINCULO = Set.of(
			"TUTOR_LEGAL",
			"CUIDADOR",
			"RESPONSAVEL_CLINICO",
			"RESPONSAVEL_OPERACIONAL",
			"CONTATO_EMERGENCIA"
	);

	private final AnimalResponsavelRepository animalResponsavelRepository;
	private final AnimalRepository animalRepository;
	private final ResponsavelRepository responsavelRepository;

	public AnimalResponsavelService(
			AnimalResponsavelRepository animalResponsavelRepository,
			AnimalRepository animalRepository,
			ResponsavelRepository responsavelRepository
	) {
		this.animalResponsavelRepository = animalResponsavelRepository;
		this.animalRepository = animalRepository;
		this.responsavelRepository = responsavelRepository;
	}

	@Transactional
	public AnimalResponsavelResponse criar(AnimalResponsavelRequest request) {
		LocalDate dataInicio = request.dataInicio() == null ? LocalDate.now() : request.dataInicio();
		AnimalResponsavelId id = criarId(request.animalId(), request.responsavelId(), dataInicio);
		if (animalResponsavelRepository.existsById(id)) {
			throw new BusinessException("Vinculo entre animal, responsavel e data de inicio ja existe.");
		}

		Animal animal = buscarAnimal(request.animalId());
		Responsavel responsavel = buscarResponsavel(request.responsavelId());
		AnimalResponsavel animalResponsavel = new AnimalResponsavel();
		animalResponsavel.setId(id);
		animalResponsavel.setAnimal(animal);
		animalResponsavel.setResponsavel(responsavel);
		aplicarDados(animalResponsavel, request, dataInicio, true);
		ajustarOutrosPrincipais(animalResponsavel);
		return AnimalResponsavelResponse.fromEntity(animalResponsavelRepository.save(animalResponsavel));
	}

	@Transactional(readOnly = true)
	public Page<AnimalResponsavelResponse> listar(Long animalId, Long responsavelId, String tipoVinculo, String ativo, Pageable pageable) {
		validarTipoQuandoInformado(tipoVinculo);
		validarSNQuandoInformado(ativo, "Ativo");
		return animalResponsavelRepository.buscar(animalId, responsavelId, vazioParaNulo(tipoVinculo), vazioParaNulo(ativo), pageable)
				.map(AnimalResponsavelResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public List<AnimalResponsavelResponse> listarAtivosPorAnimal(Long animalId) {
		buscarAnimal(animalId);
		return animalResponsavelRepository.listarPorAnimalEAtivo(animalId, "S").stream()
				.map(AnimalResponsavelResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public Page<AnimalResponsavelResponse> listarPorResponsavel(Long responsavelId, Pageable pageable) {
		buscarResponsavel(responsavelId);
		return animalResponsavelRepository.listarPorResponsavel(responsavelId, pageable)
				.map(AnimalResponsavelResponse::fromEntity);
	}

	@Transactional
	public AnimalResponsavelResponse atualizar(AnimalResponsavelRequest request) {
		LocalDate dataInicio = validarDataInicioObrigatoria(request.dataInicio());
		AnimalResponsavel animalResponsavel = buscarEntidade(request.animalId(), request.responsavelId(), dataInicio);
		aplicarDados(animalResponsavel, request, dataInicio, false);
		ajustarOutrosPrincipais(animalResponsavel);
		return AnimalResponsavelResponse.fromEntity(animalResponsavelRepository.save(animalResponsavel));
	}

	@Transactional
	public AnimalResponsavelResponse encerrar(AnimalResponsavelRequest request) {
		LocalDate dataInicio = validarDataInicioObrigatoria(request.dataInicio());
		if (request.dataFim() == null) {
			throw new BusinessException("Data fim deve ser informada para encerrar o vinculo.");
		}
		AnimalResponsavel animalResponsavel = buscarEntidade(request.animalId(), request.responsavelId(), dataInicio);
		validarDataFim(dataInicio, request.dataFim());
		animalResponsavel.setDataFim(request.dataFim());
		animalResponsavel.setAtivo("N");
		return AnimalResponsavelResponse.fromEntity(animalResponsavelRepository.save(animalResponsavel));
	}

	@Transactional
	public void excluir(Long animalId, Long responsavelId, LocalDate dataInicio) {
		AnimalResponsavel animalResponsavel = buscarEntidade(animalId, responsavelId, dataInicio);
		animalResponsavel.setAtivo("N");
		animalResponsavelRepository.save(animalResponsavel);
	}

	private AnimalResponsavel buscarEntidade(Long animalId, Long responsavelId, LocalDate dataInicio) {
		return animalResponsavelRepository.findById(criarId(animalId, responsavelId, dataInicio))
				.orElseThrow(() -> new ResourceNotFoundException("Vinculo entre animal e responsavel nao encontrado."));
	}

	private Animal buscarAnimal(Long animalId) {
		return animalRepository.findById(animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Responsavel buscarResponsavel(Long responsavelId) {
		return responsavelRepository.findById(responsavelId)
				.orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
	}

	private AnimalResponsavelId criarId(Long animalId, Long responsavelId, LocalDate dataInicio) {
		AnimalResponsavelId id = new AnimalResponsavelId();
		id.setAnimalId(animalId);
		id.setResponsavelId(responsavelId);
		id.setDataInicio(dataInicio);
		return id;
	}

	private void aplicarDados(AnimalResponsavel animalResponsavel, AnimalResponsavelRequest request, LocalDate dataInicio, boolean criando) {
		String principal = criando && request.principal() == null ? "N" : request.principal();
		String ativo = criando && request.ativo() == null ? "S" : request.ativo();
		validarTipoObrigatorio(request.tipoVinculo());
		validarSNQuandoInformado(principal, "Principal");
		validarSNQuandoInformado(ativo, "Ativo");
		validarDataFim(dataInicio, request.dataFim());
		animalResponsavel.setTipoVinculo(request.tipoVinculo());
		animalResponsavel.setDataFim(request.dataFim());
		animalResponsavel.setPrincipal(principal == null ? animalResponsavel.getPrincipal() : principal);
		animalResponsavel.setAtivo(ativo == null ? animalResponsavel.getAtivo() : ativo);
	}

	private void ajustarOutrosPrincipais(AnimalResponsavel animalResponsavel) {
		if (!"S".equals(animalResponsavel.getPrincipal()) || !"S".equals(animalResponsavel.getAtivo())) {
			return;
		}
		List<AnimalResponsavel> principais = animalResponsavelRepository.listarPrincipaisAtivos(
				animalResponsavel.getAnimal().getId(),
				"S",
				"S"
		);
		for (AnimalResponsavel principalAtual : principais) {
			if (!principalAtual.getId().equals(animalResponsavel.getId())) {
				principalAtual.setPrincipal("N");
			}
		}
		animalResponsavelRepository.saveAll(principais);
	}

	private void validarTipoObrigatorio(String tipoVinculo) {
		if (!TIPOS_VINCULO.contains(tipoVinculo)) {
			throw new BusinessException("Tipo de vinculo invalido.");
		}
	}

	private void validarTipoQuandoInformado(String tipoVinculo) {
		if (tipoVinculo != null && !tipoVinculo.isBlank() && !TIPOS_VINCULO.contains(tipoVinculo)) {
			throw new BusinessException("Tipo de vinculo invalido.");
		}
	}

	private void validarSNQuandoInformado(String valor, String campo) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException(campo + " deve ser S ou N.");
		}
	}

	private void validarDataFim(LocalDate dataInicio, LocalDate dataFim) {
		if (dataFim != null && dataFim.isBefore(dataInicio)) {
			throw new BusinessException("Data fim nao pode ser anterior a data inicio.");
		}
	}

	private LocalDate validarDataInicioObrigatoria(LocalDate dataInicio) {
		if (dataInicio == null) {
			throw new BusinessException("Data inicio deve ser informada.");
		}
		return dataInicio;
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
