package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.IndicadoresDatabaseException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.IndicadoresOracleRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IndicadoresServiceTest {

	private AnimalRepository animais;
	private ClinicalAccessService clinicalAccessService;
	private IndicadoresOracleRepository indicadoresOracle;
	private IndicadoresService service;
	private Animal animal;
	private UsuarioPrincipal responsavel;

	@BeforeEach
	void setUp() {
		animais = mock(AnimalRepository.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		indicadoresOracle = mock(IndicadoresOracleRepository.class);
		service = new IndicadoresService(animais, clinicalAccessService, indicadoresOracle);

		animal = new Animal();
		animal.setId(1L);
		responsavel = principal(TipoUsuario.RESPONSAVEL);
		when(animais.findById(1L)).thenReturn(Optional.of(animal));
	}

	@Test
	void retornaOsDoisIndicadoresReaisDoAnimalAposAutorizar() {
		when(indicadoresOracle.totalConsultasAnimal(1L)).thenReturn(4L);
		when(indicadoresOracle.totalConsultasFinalizadas(1L)).thenReturn(3L);

		var response = service.buscarConsultasAnimal(1L, responsavel);

		assertEquals(1L, response.animalId());
		assertEquals(4L, response.totalConsultas());
		assertEquals(3L, response.totalConsultasFinalizadas());
		InOrder ordem = inOrder(clinicalAccessService, indicadoresOracle);
		ordem.verify(clinicalAccessService).exigirLeituraAnimal(responsavel, animal);
		ordem.verify(indicadoresOracle).totalConsultasAnimal(1L);
		ordem.verify(indicadoresOracle).totalConsultasFinalizadas(1L);
	}

	@Test
	void animalInexistenteNaoExecutaProcedure() {
		when(animais.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> service.buscarConsultasAnimal(99L, responsavel));

		verifyNoInteractions(indicadoresOracle);
	}

	@Test
	void usuarioSemAcessoNaoExecutaProcedure() {
		org.mockito.Mockito.doThrow(new AccessDeniedException("sem acesso"))
				.when(clinicalAccessService).exigirLeituraAnimal(responsavel, animal);

		assertThrows(AccessDeniedException.class,
				() -> service.buscarConsultasAnimal(1L, responsavel));

		verifyNoInteractions(indicadoresOracle);
	}

	@Test
	void falhaOracleEhPropagadaSemExecutarSegundaProcedure() {
		when(indicadoresOracle.totalConsultasAnimal(1L))
				.thenThrow(new IndicadoresDatabaseException());

		assertThrows(IndicadoresDatabaseException.class,
				() -> service.buscarConsultasAnimal(1L, responsavel));

		verify(indicadoresOracle, never()).totalConsultasFinalizadas(1L);
	}

	@Test
	void sysadminPodeConsultarTotalGlobal() {
		when(indicadoresOracle.totalPacientesAtivos()).thenReturn(52L);

		var response = service.buscarPacientesAtivos(principal(TipoUsuario.SYSADMIN));

		assertEquals(52L, response.totalPacientesAtivos());
	}

	@Test
	void perfilNaoSysadminNaoExecutaProcedureGlobal() {
		assertThrows(AccessDeniedException.class,
				() -> service.buscarPacientesAtivos(responsavel));

		verify(indicadoresOracle, never()).totalPacientesAtivos();
	}

	private UsuarioPrincipal principal(TipoUsuario tipo) {
		return new UsuarioPrincipal(10L, "Usuario", "usuario@arkive.com", "hash",
				tipo, "S", false, 20L, 30L, 40L);
	}
}
