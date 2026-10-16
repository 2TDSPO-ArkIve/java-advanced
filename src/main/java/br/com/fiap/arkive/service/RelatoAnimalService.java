package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.CriarRelatoAnimalRequest;
import br.com.fiap.arkive.dto.response.RelatoAnimalResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.RelatoAnimal;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.RelatoAnimalRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("!local-nodb")
public class RelatoAnimalService {

	private static final int MAX_TEXTO_LENGTH = 2000;
	private static final int MAX_BUSCA_LENGTH = 100;

	private final RelatoAnimalRepository relatoRepository;
	private final AnimalRepository animalRepository;
	private final ResponsavelRepository responsavelRepository;
	private final ClinicalAccessService clinicalAccessService;
	private final RelatoSearchNormalizer searchNormalizer;
	private final Clock clock;

	public RelatoAnimalService(
			RelatoAnimalRepository relatoRepository,
			AnimalRepository animalRepository,
			ResponsavelRepository responsavelRepository,
			ClinicalAccessService clinicalAccessService,
			RelatoSearchNormalizer searchNormalizer,
			@Qualifier("businessClock") Clock clock
	) {
		this.relatoRepository = relatoRepository;
		this.animalRepository = animalRepository;
		this.responsavelRepository = responsavelRepository;
		this.clinicalAccessService = clinicalAccessService;
		this.searchNormalizer = searchNormalizer;
		this.clock = clock;
	}

	@Transactional
	public RelatoAnimalResponse criar(
			Long animalId,
			CriarRelatoAnimalRequest request,
			UsuarioPrincipal principal
	) {
		LocalDate hoje = LocalDate.now(clock);
		Animal animal = buscarAnimal(animalId);
		clinicalAccessService.exigirCriacaoRelatoResponsavel(principal, animal, hoje);
		Responsavel autor = buscarAutorAtivo(principal);
		if (!"S".equals(animal.getAtivo())) {
			throw new BusinessException("Nao e possivel criar relato para um animal inativo.", HttpStatus.CONFLICT);
		}

		String texto = validarTexto(request == null ? null : request.texto());
		Boolean origemTranscricao = request == null ? null : request.originadoDeTranscricao();
		if (origemTranscricao == null) {
			throw new BusinessException("Origem de transcricao e obrigatoria.");
		}
		LocalDate dataOcorrencia = request.dataOcorrencia();
		if (dataOcorrencia != null && dataOcorrencia.isAfter(hoje)) {
			throw new BusinessException("Data de ocorrencia nao pode estar no futuro.");
		}

		RelatoAnimal relato = new RelatoAnimal();
		relato.setAnimal(animal);
		relato.setAutor(autor);
		relato.setTexto(texto);
		relato.setTextoBusca(searchNormalizer.normalize(texto));
		relato.setDataCriacao(LocalDateTime.now(clock));
		relato.setDataOcorrencia(dataOcorrencia);
		relato.setOrigemTranscricao(origemTranscricao ? "S" : "N");
		return RelatoAnimalResponse.fromEntity(relatoRepository.save(relato));
	}

	@Transactional(readOnly = true)
	public Page<RelatoAnimalResponse> listar(
			Long animalId,
			String busca,
			int page,
			int size,
			UsuarioPrincipal principal
	) {
		Animal animal = buscarAnimal(animalId);
		clinicalAccessService.exigirLeituraAnimal(principal, animal);
		validarPaginacaoEBusca(busca, page, size);

		String buscaNormalizada = searchNormalizer.normalize(busca);
		PageRequest pageable = PageRequest.of(page, size);
		Page<Long> ids = buscaNormalizada == null
				? relatoRepository.listarIdsPorAnimal(animalId, pageable)
				: relatoRepository.buscarIdsPorAnimal(animalId, buscaNormalizada, pageable);
		if (ids.isEmpty()) {
			return new PageImpl<>(Collections.emptyList(), pageable, ids.getTotalElements());
		}

		Map<Long, RelatoAnimal> porId = relatoRepository.buscarDetalhesPorIds(ids.getContent()).stream()
				.collect(Collectors.toMap(RelatoAnimal::getId, Function.identity()));
		List<RelatoAnimalResponse> respostas = ids.getContent().stream()
				.map(porId::get)
				.filter(java.util.Objects::nonNull)
				.map(RelatoAnimalResponse::fromEntity)
				.toList();
		return new PageImpl<>(respostas, pageable, ids.getTotalElements());
	}

	private Animal buscarAnimal(Long animalId) {
		return animalRepository.findById(animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Responsavel buscarAutorAtivo(UsuarioPrincipal principal) {
		if (principal == null || principal.getResponsavelId() == null) {
			throw new AccessDeniedException("Responsavel autenticado invalido.");
		}
		Responsavel autor = responsavelRepository.findById(principal.getResponsavelId())
				.orElseThrow(() -> new AccessDeniedException("Responsavel autenticado invalido."));
		if (!"S".equals(autor.getAtivo())) {
			throw new AccessDeniedException("Responsavel inativo nao pode criar relatos.");
		}
		return autor;
	}

	private String validarTexto(String texto) {
		if (texto == null || texto.trim().isEmpty()) {
			throw new BusinessException("Texto do relato e obrigatorio.");
		}
		String normalizado = texto.trim();
		if (normalizado.length() > MAX_TEXTO_LENGTH) {
			throw new BusinessException("Texto do relato deve ter no maximo 2000 caracteres.");
		}
		return normalizado;
	}

	private void validarPaginacaoEBusca(String busca, int page, int size) {
		if (page < 0) {
			throw new BusinessException("Pagina deve ser maior ou igual a zero.");
		}
		if (size < 1 || size > 50) {
			throw new BusinessException("Tamanho da pagina deve estar entre 1 e 50.");
		}
		if (busca != null && busca.length() > MAX_BUSCA_LENGTH) {
			throw new BusinessException("Busca deve ter no maximo 100 caracteres.");
		}
	}
}
