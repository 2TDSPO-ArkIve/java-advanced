package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.CriarRelatoAnimalRequest;
import br.com.fiap.arkive.dto.response.RelatoAnimalResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.RelatoAnimal;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.RelatoAnimalRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelatoAnimalServiceTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T12:15:22Z"), SAO_PAULO);

	private RelatoAnimalRepository relatoRepository;
	private AnimalRepository animalRepository;
	private ResponsavelRepository responsavelRepository;
	private ClinicalAccessService clinicalAccessService;
	private RelatoAnimalService service;

	@BeforeEach
	void setUp() {
		relatoRepository = mock(RelatoAnimalRepository.class);
		animalRepository = mock(AnimalRepository.class);
		responsavelRepository = mock(ResponsavelRepository.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		service = new RelatoAnimalService(relatoRepository, animalRepository, responsavelRepository,
				clinicalAccessService, new RelatoSearchNormalizer(), CLOCK);
	}

	@Test
	void criaRelatoComAutorDaSessaoTextoOriginalNormalizadoEHorarioDeNegocio() {
		Animal animal = animalAtivo();
		animal.setObservacaoResponsavelPendente("Observacao independente");
		Responsavel autor = responsavelAtivo(40L, "Maria Silva");
		prepararCriacao(animal, autor);
		when(relatoRepository.save(any())).thenAnswer(invocation -> {
			RelatoAnimal salvo = invocation.getArgument(0);
			salvo.setId(901L);
			return salvo;
		});

		RelatoAnimalResponse response = service.criar(123L,
				new CriarRelatoAnimalRequest("  Vomitou duas vezes durante a madrugada.  ",
						LocalDate.of(2026, 10, 15), true), responsavelPrincipal(40L));

		ArgumentCaptor<RelatoAnimal> captor = ArgumentCaptor.forClass(RelatoAnimal.class);
		verify(relatoRepository).save(captor.capture());
		RelatoAnimal salvo = captor.getValue();
		assertEquals(40L, salvo.getAutor().getId());
		assertEquals("Vomitou duas vezes durante a madrugada.", salvo.getTexto());
		assertEquals("VOMITOU DUAS VEZES DURANTE A MADRUGADA.", salvo.getTextoBusca());
		assertEquals(LocalDateTime.of(2026, 10, 16, 9, 15, 22), salvo.getDataCriacao());
		assertEquals("S", salvo.getOrigemTranscricao());
		assertEquals("Observacao independente", animal.getObservacaoResponsavelPendente());
		assertEquals(901L, response.id());
		assertEquals("ARK-PET-TESTE", response.animalArkivePatientId());
		assertEquals("Maria Silva", response.responsavelNome());
		assertTrue(response.originadoDeTranscricao());
	}

	@Test
	void permiteMultiplosRelatosEOrigemNaoTranscrita() {
		Animal animal = animalAtivo();
		Responsavel autor = responsavelAtivo(40L, "Maria Silva");
		prepararCriacao(animal, autor);
		when(relatoRepository.save(any())).thenAnswer(invocation -> {
			RelatoAnimal salvo = invocation.getArgument(0);
			salvo.setId("Primeiro".equals(salvo.getTexto()) ? 1L : 2L);
			return salvo;
		});

		RelatoAnimalResponse primeiro = service.criar(123L,
				new CriarRelatoAnimalRequest("Primeiro", null, false), responsavelPrincipal(40L));
		RelatoAnimalResponse segundo = service.criar(123L,
				new CriarRelatoAnimalRequest("Segundo", null, false), responsavelPrincipal(40L));

		assertEquals(1L, primeiro.id());
		assertEquals(2L, segundo.id());
		assertFalse(segundo.originadoDeTranscricao());
		verify(relatoRepository, org.mockito.Mockito.times(2)).save(any());
	}

	@Test
	void aceitaExatamenteDoisMilCaracteresEDataOpcional() {
		prepararCriacao(animalAtivo(), responsavelAtivo(40L, "Maria"));
		when(relatoRepository.save(any())).thenAnswer(invocation -> {
			RelatoAnimal salvo = invocation.getArgument(0);
			salvo.setId(1L);
			return salvo;
		});

		RelatoAnimalResponse response = service.criar(123L,
				new CriarRelatoAnimalRequest("a".repeat(2000), null, false), responsavelPrincipal(40L));

		assertEquals(2000, response.texto().length());
		assertNull(response.dataOcorrencia());
	}

	@Test
	void rejeitaTextoVazioAcimaDoLimiteFlagAusenteEDataFutura() {
		prepararCriacao(animalAtivo(), responsavelAtivo(40L, "Maria"));

		assertThrows(BusinessException.class, () -> service.criar(123L,
				new CriarRelatoAnimalRequest("  ", null, false), responsavelPrincipal(40L)));
		assertThrows(BusinessException.class, () -> service.criar(123L,
				new CriarRelatoAnimalRequest("a".repeat(2001), null, false), responsavelPrincipal(40L)));
		assertThrows(BusinessException.class, () -> service.criar(123L,
				new CriarRelatoAnimalRequest("Relato", null, null), responsavelPrincipal(40L)));
		assertThrows(BusinessException.class, () -> service.criar(123L,
				new CriarRelatoAnimalRequest("Relato", LocalDate.of(2026, 10, 17), false), responsavelPrincipal(40L)));
		verify(relatoRepository, never()).save(any());
	}

	@Test
	void rejeitaAnimalInativoComConflito() {
		Animal animal = animalAtivo();
		animal.setAtivo("N");
		prepararCriacao(animal, responsavelAtivo(40L, "Maria"));

		BusinessException exception = assertThrows(BusinessException.class, () -> service.criar(123L,
				new CriarRelatoAnimalRequest("Relato", null, false), responsavelPrincipal(40L)));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		verify(relatoRepository, never()).save(any());
	}

	@Test
	void autorizacaoDeCriacaoExigeVinculoAtualEAutorAtivo() {
		Animal animal = animalAtivo();
		when(animalRepository.findById(123L)).thenReturn(Optional.of(animal));
		doThrow(new AccessDeniedException("negado")).when(clinicalAccessService)
				.exigirCriacaoRelatoResponsavel(any(), eq(animal), eq(LocalDate.of(2026, 10, 16)));

		assertThrows(AccessDeniedException.class, () -> service.criar(123L,
				new CriarRelatoAnimalRequest("Relato", null, false), responsavelPrincipal(99L)));

		Responsavel inativo = responsavelAtivo(40L, "Maria");
		inativo.setAtivo("N");
		when(responsavelRepository.findById(40L)).thenReturn(Optional.of(inativo));
		org.mockito.Mockito.reset(clinicalAccessService);
		assertThrows(AccessDeniedException.class, () -> service.criar(123L,
				new CriarRelatoAnimalRequest("Relato", null, false), responsavelPrincipal(40L)));
		verify(relatoRepository, never()).save(any());
	}

	@Test
	void listaComAutorizacaoBuscaNormalizadaPaginacaoTotalEOrdemEstavel() {
		Animal animal = animalAtivo();
		when(animalRepository.findById(123L)).thenReturn(Optional.of(animal));
		PageRequest pageable = PageRequest.of(0, 2);
		when(relatoRepository.buscarIdsPorAnimal(123L, "VOMITO", pageable))
				.thenReturn(new PageImpl<>(List.of(20L, 10L), pageable, 3));
		RelatoAnimal maisNovo = relato(20L, animal, responsavelAtivo(41L, "Outra autora"), "vômito recente");
		RelatoAnimal maisAntigo = relato(10L, animal, responsavelAtivo(40L, "Maria"), "vomito antigo");
		when(relatoRepository.buscarDetalhesPorIds(List.of(20L, 10L)))
				.thenReturn(List.of(maisAntigo, maisNovo));

		UsuarioPrincipal principal = veterinarioPrincipal();
		var result = service.listar(123L, " vômito ", 0, 2, principal);

		verify(clinicalAccessService).exigirLeituraAnimal(principal, animal);
		assertEquals(3, result.getTotalElements());
		assertEquals(List.of(20L, 10L), result.getContent().stream().map(RelatoAnimalResponse::id).toList());
		assertEquals("Outra autora", result.getContent().get(0).responsavelNome());
	}

	@Test
	void buscaVaziaUsaListagemSemFiltroEValidaLimites() {
		Animal animal = animalAtivo();
		when(animalRepository.findById(123L)).thenReturn(Optional.of(animal));
		when(relatoRepository.listarIdsPorAnimal(123L, PageRequest.of(0, 20)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		assertTrue(service.listar(123L, "  ", 0, 20, responsavelPrincipal(40L)).isEmpty());
		assertThrows(BusinessException.class,
				() -> service.listar(123L, "a".repeat(101), 0, 20, responsavelPrincipal(40L)));
		assertThrows(BusinessException.class,
				() -> service.listar(123L, null, -1, 20, responsavelPrincipal(40L)));
		assertThrows(BusinessException.class,
				() -> service.listar(123L, null, 0, 51, responsavelPrincipal(40L)));
	}

	private void prepararCriacao(Animal animal, Responsavel autor) {
		when(animalRepository.findById(123L)).thenReturn(Optional.of(animal));
		when(responsavelRepository.findById(autor.getId())).thenReturn(Optional.of(autor));
	}

	private Animal animalAtivo() {
		Animal animal = new Animal();
		animal.setId(123L);
		animal.assignArkivePatientId("ARK-PET-TESTE");
		animal.setNome("Epona");
		animal.setAtivo("S");
		return animal;
	}

	private Responsavel responsavelAtivo(Long id, String nome) {
		Responsavel responsavel = new Responsavel();
		responsavel.setId(id);
		responsavel.setNome(nome);
		responsavel.setAtivo("S");
		return responsavel;
	}

	private RelatoAnimal relato(Long id, Animal animal, Responsavel autor, String texto) {
		RelatoAnimal relato = new RelatoAnimal();
		relato.setId(id);
		relato.setAnimal(animal);
		relato.setAutor(autor);
		relato.setTexto(texto);
		relato.setTextoBusca(new RelatoSearchNormalizer().normalize(texto));
		relato.setDataCriacao(LocalDateTime.of(2026, 10, 16, 9, 0));
		relato.setOrigemTranscricao("N");
		return relato;
	}

	private UsuarioPrincipal responsavelPrincipal(Long responsavelId) {
		return new UsuarioPrincipal(2L, "Maria", "maria@test", "hash", TipoUsuario.RESPONSAVEL,
				"S", false, responsavelId, null, null);
	}

	private UsuarioPrincipal veterinarioPrincipal() {
		return new UsuarioPrincipal(3L, "Vet", "vet@test", "hash", TipoUsuario.VETERINARIO,
				"S", false, null, 10L, null);
	}
}
