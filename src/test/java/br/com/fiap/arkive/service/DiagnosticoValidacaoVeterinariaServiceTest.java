package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.DiagnosticoResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.DoencaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiagnosticoValidacaoVeterinariaServiceTest {

	private DiagnosticoRepository diagnosticoRepository;
	private DiagnosticoService diagnosticoService;

	@BeforeEach
	void setUp() {
		diagnosticoRepository = mock(DiagnosticoRepository.class);
		ClinicalAccessService clinicalAccessService = new ClinicalAccessService(
				mock(AnimalResponsavelRepository.class),
				mock(ConsultaRepository.class),
				mock(VeterinarioService.class)
		);
		diagnosticoService = new DiagnosticoService(
				diagnosticoRepository,
				mock(ConsultaService.class),
				mock(DoencaRepository.class),
				clinicalAccessService
		);
		when(diagnosticoRepository.save(any(Diagnostico.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void veterinarioResponsavelMarcaInsightComoUtilERespostaRetornaS() {
		Diagnostico diagnostico = suporteIa();
		when(diagnosticoRepository.findById(50L)).thenReturn(Optional.of(diagnostico));

		DiagnosticoResponse response = diagnosticoService.atualizarValidacaoVeterinaria(50L, true, veterinario(10L));

		assertEquals("S", diagnostico.getValidacaoVet());
		assertEquals("S", response.validacaoVet());
		assertEquals("N", response.confirmado());
		verify(diagnosticoRepository).save(diagnostico);
	}

	@Test
	void veterinarioResponsavelMarcaInsightComoNaoUtil() {
		Diagnostico diagnostico = suporteIa();
		when(diagnosticoRepository.findById(50L)).thenReturn(Optional.of(diagnostico));

		diagnosticoService.atualizarValidacaoVeterinaria(50L, false, veterinario(10L));

		assertEquals("N", diagnostico.getValidacaoVet());
		assertEquals("N", diagnostico.getConfirmado());
	}

	@Test
	void veterinarioPodeMudarAvaliacaoNosDoisSentidos() {
		Diagnostico diagnostico = suporteIa();
		diagnostico.setValidacaoVet("S");
		when(diagnosticoRepository.findById(50L)).thenReturn(Optional.of(diagnostico));

		diagnosticoService.atualizarValidacaoVeterinaria(50L, false, veterinario(10L));
		assertEquals("N", diagnostico.getValidacaoVet());

		diagnosticoService.atualizarValidacaoVeterinaria(50L, true, veterinario(10L));
		assertEquals("S", diagnostico.getValidacaoVet());
	}

	@Test
	void repetirMesmaAvaliacaoEhIdempotente() {
		Diagnostico diagnostico = suporteIa();
		diagnostico.setValidacaoVet("S");
		when(diagnosticoRepository.findById(50L)).thenReturn(Optional.of(diagnostico));

		DiagnosticoResponse response = diagnosticoService.atualizarValidacaoVeterinaria(50L, true, veterinario(10L));

		assertEquals("S", diagnostico.getValidacaoVet());
		assertEquals("S", response.validacaoVet());
	}

	@Test
	void veterinarioNaoRelacionadoEhRejeitado() {
		when(diagnosticoRepository.findById(50L)).thenReturn(Optional.of(suporteIa()));

		assertThrows(AccessDeniedException.class,
				() -> diagnosticoService.atualizarValidacaoVeterinaria(50L, true, veterinario(22L)));

		verify(diagnosticoRepository, never()).save(any());
	}

	@Test
	void responsavelEhRejeitado() {
		when(diagnosticoRepository.findById(50L)).thenReturn(Optional.of(suporteIa()));

		assertThrows(AccessDeniedException.class,
				() -> diagnosticoService.atualizarValidacaoVeterinaria(50L, true, responsavel()));

		verify(diagnosticoRepository, never()).save(any());
	}

	@Test
	void diagnosticoNaoGeradoPorIaEhRejeitadoCom422() {
		Diagnostico diagnostico = suporteIa();
		diagnostico.setConfirmado("S");
		diagnostico.setInsightIa(null);
		diagnostico.setConfianca(null);
		when(diagnosticoRepository.findById(50L)).thenReturn(Optional.of(diagnostico));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> diagnosticoService.atualizarValidacaoVeterinaria(50L, true, veterinario(10L)));

		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
		verify(diagnosticoRepository, never()).save(any());
	}

	private Diagnostico suporteIa() {
		Animal animal = new Animal();
		animal.setId(90L);
		Veterinario veterinario = new Veterinario();
		veterinario.setId(10L);
		Consulta consulta = new Consulta();
		consulta.setId(60L);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setId(50L);
		diagnostico.setDiagnostico("Hipotese");
		diagnostico.setConfirmado("N");
		diagnostico.setInsightIa("Insight clinico");
		diagnostico.setConfianca(BigDecimal.valueOf(80));
		diagnostico.setConsulta(consulta);
		return diagnostico;
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "hash", TipoUsuario.VETERINARIO,
				"S", false, null, veterinarioId, null);
	}

	private UsuarioPrincipal responsavel() {
		return new UsuarioPrincipal(2L, "Responsável", "responsavel@arkive.com", "hash", TipoUsuario.RESPONSAVEL,
				"S", false, 40L, null, null);
	}
}
