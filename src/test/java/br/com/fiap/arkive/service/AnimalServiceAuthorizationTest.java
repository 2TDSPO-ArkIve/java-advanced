package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimalServiceAuthorizationTest {

	private AnimalRepository animalRepository;
	private EspecieService especieService;
	private RacaService racaService;
	private ClinicaService clinicaService;
	private EventoJornadaService eventoJornadaService;
	private ClinicalAccessService clinicalAccessService;
	private AnimalService animalService;

	@BeforeEach
	void setUp() {
		animalRepository = mock(AnimalRepository.class);
		especieService = mock(EspecieService.class);
		racaService = mock(RacaService.class);
		clinicaService = mock(ClinicaService.class);
		eventoJornadaService = mock(EventoJornadaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		animalService = new AnimalService(
				animalRepository,
				especieService,
				racaService,
				clinicaService,
				eventoJornadaService,
				clinicalAccessService
		);
		when(especieService.buscarEntidade(1L)).thenReturn(especie());
		when(clinicaService.buscarEntidade(30L)).thenReturn(clinica(30L));
		when(clinicaService.buscarEntidade(31L)).thenReturn(clinica(31L));
		when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(eventoJornadaService.criarPayload(any(), any(), any())).thenReturn("{\"entity\":\"Animal\"}");
	}

	@Test
	void responsavelListaSomenteAnimaisVinculados() {
		UsuarioPrincipal principal = principal(TipoUsuario.RESPONSAVEL, 40L, null, null);
		when(animalRepository.buscarParaResponsavel(eq(40L), any(LocalDate.class), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
				.thenReturn(Page.empty());

		animalService.listarAutorizado(null, null, null, null, null, Pageable.unpaged(), principal);

		verify(animalRepository).buscarParaResponsavel(eq(40L), any(LocalDate.class), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
	}

	@Test
	void veterinarioListaSomenteAnimaisComConsultasProprias() {
		UsuarioPrincipal principal = principal(TipoUsuario.VETERINARIO, null, 10L, null);
		when(animalRepository.buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
				.thenReturn(Page.empty());

		animalService.listarAutorizado(null, null, null, null, null, Pageable.unpaged(), principal);

		verify(animalRepository).buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
	}

	@Test
	void adminClinicaComFiltroConflitanteRecebeListaVazia() {
		UsuarioPrincipal principal = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);

		Page<AnimalResponse> resultado = animalService.listarAutorizado(null, null, null, 31L, null, Pageable.unpaged(), principal);

		assertEquals(0, resultado.getTotalElements());
	}

	@Test
	void buscaAnimalPorIdExigeAcessoAoRecurso() {
		Animal animal = animal();
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal));
		UsuarioPrincipal principal = principal(TipoUsuario.RESPONSAVEL, 40L, null, null);

		animalService.buscarPorIdAutorizado(50L, principal);

		verify(clinicalAccessService).exigirLeituraAnimal(principal, animal);
	}

	@Test
	void sysadminCriaAnimalGlobalmente() {
		animalService.criar(request(31L), principal(TipoUsuario.SYSADMIN, null, null, null));

		verify(animalRepository).save(argThat(animal -> animal.getClinica().getId().equals(31L)));
	}

	@Test
	void sysadminAtualizaAnimalGlobalmente() {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(31L)));

		animalService.atualizar(50L, request(30L), principal(TipoUsuario.SYSADMIN, null, null, null));

		verify(animalRepository).save(argThat(animal -> animal.getClinica().getId().equals(30L)));
	}

	@Test
	void sysadminExcluiAnimalGlobalmente() {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(31L)));

		animalService.excluir(50L, principal(TipoUsuario.SYSADMIN, null, null, null));

		verify(animalRepository).save(argThat(animal -> "N".equals(animal.getAtivo())));
	}

	@Test
	void adminClinicaCriaSomenteNaPropriaClinica() {
		UsuarioPrincipal admin = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);

		animalService.criar(request(null), admin);
		animalService.criar(request(30L), admin);

		verify(animalRepository, org.mockito.Mockito.times(2)).save(argThat(animal -> animal.getClinica().getId().equals(30L)));
		assertThrows(AccessDeniedException.class, () -> animalService.criar(request(31L), admin));
	}

	@Test
	void adminClinicaAtualizaSomenteAnimalDaPropriaClinicaENaoMoveClinica() {
		UsuarioPrincipal admin = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(30L)));

		animalService.atualizar(50L, request(null), admin);

		verify(animalRepository).save(argThat(animal -> animal.getClinica().getId().equals(30L)));
		BusinessException exception = assertThrows(BusinessException.class, () -> animalService.atualizar(50L, request(31L), admin));
		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}

	@Test
	void adminClinicaNaoAtualizaOuExcluiAnimalDeOutraClinica() {
		UsuarioPrincipal admin = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(31L)));

		assertThrows(AccessDeniedException.class, () -> animalService.atualizar(50L, request(31L), admin));
		assertThrows(AccessDeniedException.class, () -> animalService.excluir(50L, admin));

		verify(animalRepository, never()).save(any());
	}

	@Test
	void adminClinicaExcluiAnimalDaPropriaClinica() {
		UsuarioPrincipal admin = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(30L)));

		animalService.excluir(50L, admin);

		verify(animalRepository).save(argThat(animal -> "N".equals(animal.getAtivo())));
	}

	@Test
	void veterinarioEResponsavelNaoExecutamMutacaoGenericaDeAnimal() {
		assertMutacoesBloqueadas(principal(TipoUsuario.VETERINARIO, null, 10L, null));
		assertMutacoesBloqueadas(principal(TipoUsuario.RESPONSAVEL, 40L, null, null));
	}

	@Test
	void sobrecargasGenericasDeEscritaExigemPrincipal() {
		assertThrows(AccessDeniedException.class, () -> animalService.criar(request(30L)));
		assertThrows(AccessDeniedException.class, () -> animalService.atualizar(50L, request(30L)));
		assertThrows(AccessDeniedException.class, () -> animalService.excluir(50L));
	}

	private Animal animal() {
		return animal(30L);
	}

	private Animal animal(Long clinicaId) {
		Especie especie = new Especie();
		especie.setId(1L);
		especie.setNome("Canino");
		Animal animal = new Animal();
		animal.setId(50L);
		animal.setNome("Nina");
		animal.setEspecie(especie);
		animal.setClinica(clinica(clinicaId));
		animal.setAtivo("S");
		return animal;
	}

	private Especie especie() {
		Especie especie = new Especie();
		especie.setId(1L);
		especie.setNome("Canino");
		return especie;
	}

	private Clinica clinica(Long id) {
		Clinica clinica = new Clinica();
		clinica.setId(id);
		clinica.setNome("Clinica " + id);
		return clinica;
	}

	private AnimalRequest request(Long clinicaId) {
		return new AnimalRequest("Nina", 1L, null, "F", "N", clinicaId, "S");
	}

	private void assertMutacoesBloqueadas(UsuarioPrincipal principal) {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(30L)));

		assertThrows(AccessDeniedException.class, () -> animalService.criar(request(30L), principal));
		assertThrows(AccessDeniedException.class, () -> animalService.atualizar(50L, request(30L), principal));
		assertThrows(AccessDeniedException.class, () -> animalService.excluir(50L, principal));
	}

	private UsuarioPrincipal principal(TipoUsuario tipo, Long responsavelId, Long veterinarioId, Long clinicaId) {
		return new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "$2a$10$hash", tipo, "S", false, responsavelId, veterinarioId, clinicaId);
	}

}
