package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimalServiceAuthorizationTest {

	private AnimalRepository animalRepository;
	private ClinicalAccessService clinicalAccessService;
	private AnimalService animalService;

	@BeforeEach
	void setUp() {
		animalRepository = mock(AnimalRepository.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		animalService = new AnimalService(
				animalRepository,
				mock(EspecieService.class),
				mock(RacaService.class),
				mock(ClinicaService.class),
				mock(EventoJornadaService.class),
				clinicalAccessService
		);
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

	private Animal animal() {
		Especie especie = new Especie();
		especie.setId(1L);
		especie.setNome("Canino");
		Animal animal = new Animal();
		animal.setId(50L);
		animal.setNome("Nina");
		animal.setEspecie(especie);
		Clinica clinica = new Clinica();
		clinica.setId(30L);
		animal.setClinica(clinica);
		return animal;
	}

	private UsuarioPrincipal principal(TipoUsuario tipo, Long responsavelId, Long veterinarioId, Long clinicaId) {
		return new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "$2a$10$hash", tipo, "S", false, responsavelId, veterinarioId, clinicaId);
	}

}
