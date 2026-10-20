package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.ClinicalSupportResponse;
import br.com.fiap.arkive.dto.response.RagReferenceResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.clinical.ClinicalSupportProviderResult;
import br.com.fiap.arkive.service.clinical.ClinicalSupportTokenUsage;
import br.com.fiap.arkive.service.clinical.RagReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicalSupportPersistenceServiceTest {

	private ConsultaService consultaService;
	private ConsultaRepository consultaRepository;
	private DiagnosticoService diagnosticoService;
	private ClinicalAccessService clinicalAccessService;
	private ClinicalSupportPersistenceService persistenceService;

	@BeforeEach
	void setUp() {
		consultaService = mock(ConsultaService.class);
		consultaRepository = mock(ConsultaRepository.class);
		diagnosticoService = mock(DiagnosticoService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		persistenceService = new ClinicalSupportPersistenceService(
				consultaService,
				consultaRepository,
				diagnosticoService,
				clinicalAccessService,
				new ObjectMapper()
		);
		when(consultaRepository.save(any(Consulta.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void persisteDiagnosticoIaEMoveConsultaParaApNaMesmaOperacaoTransacional() {
		Consulta consulta = consulta("EP");
		Diagnostico diagnostico = diagnosticoIa(consulta);
		UsuarioPrincipal principal = veterinario(10L);
		ClinicalSupportProviderResult result = result();
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		String referenciasJson = "[{\"fonte\":\"ABCD\",\"titulo\":\"Feline calicivirus infection\"},"
				+ "{\"fonte\":\"CFSPH\",\"titulo\":\"Cryptococcosis\"}]";
		when(diagnosticoService.criarSuporteClinicoIa(consulta, result.diagnostico(), result.severidade(), result.insightIa(), result.confianca(), "[\"https://source-one.example\",\"https://source-two.example\"]", referenciasJson, 1000L, 500L, 1500L))
				.thenReturn(diagnostico);

		ClinicalSupportResponse response = persistenceService.persistirSuporte(6L, result, principal);

		verify(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);
		verify(diagnosticoService).criarSuporteClinicoIa(consulta, result.diagnostico(), result.severidade(), result.insightIa(), result.confianca(), "[\"https://source-one.example\",\"https://source-two.example\"]", referenciasJson, 1000L, 500L, 1500L);
		verify(consultaRepository).save(consulta);
		assertEquals("AP", consulta.getStatus());
		assertEquals("AP", response.statusConsulta());
		assertEquals("Aguardando Parecer", response.statusDescricao());
		assertEquals("Hipotese", response.hipoteseDiagnostica());
		assertEquals(50L, response.diagnosticoId());
		assertNull(response.validacaoVet());
		assertEquals(1000L, response.tokens().entrada());
		assertEquals(500L, response.tokens().saida());
		assertEquals(1500L, response.tokens().total());
		assertEquals(List.of(
				new RagReferenceResponse("ABCD", "Feline calicivirus infection"),
				new RagReferenceResponse("CFSPH", "Cryptococcosis")
		), response.referenciasRag());
		assertEquals(List.of("https://source-one.example", "https://source-two.example"), response.fontesPesquisadas());
	}

	@Test
	void fontesVaziasSaoPersistidasComoNulo() {
		Consulta consulta = consulta("EP");
		Diagnostico diagnostico = diagnosticoIa(consulta);
		ClinicalSupportProviderResult result = new ClinicalSupportProviderResult("Hipotese", "MODERADA", "Insight clinico", 65, List.of());
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		when(diagnosticoService.criarSuporteClinicoIa(consulta, result.diagnostico(), result.severidade(), result.insightIa(), result.confianca(), null, null, null, null, null))
				.thenReturn(diagnostico);

		persistenceService.persistirSuporte(6L, result, veterinario(10L));

		verify(diagnosticoService).criarSuporteClinicoIa(consulta, result.diagnostico(), result.severidade(), result.insightIa(), result.confianca(), null, null, null, null, null);
	}

	@Test
	void revalidaEstadoAntesDePersistir() {
		Consulta consulta = consulta("FI");
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);

		assertThrows(BusinessException.class, () -> persistenceService.persistirSuporte(6L, result(), veterinario(10L)));

		verify(diagnosticoService, never()).criarSuporteClinicoIa(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
		verify(consultaRepository, never()).save(any());
	}

	@Test
	void revalidaAutorizacaoAntesDePersistir() {
		Consulta consulta = consulta("EP");
		UsuarioPrincipal principal = veterinario(22L);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		doThrow(new AccessDeniedException("Veterinario nao autorizado para esta consulta."))
				.when(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);

		assertThrows(AccessDeniedException.class, () -> persistenceService.persistirSuporte(6L, result(), principal));

		verify(diagnosticoService, never()).criarSuporteClinicoIa(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
		verify(consultaRepository, never()).save(any());
	}

	private ClinicalSupportProviderResult result() {
		return new ClinicalSupportProviderResult(
				"Hipotese",
				"MODERADA",
				"Insight clinico",
				65,
				List.of("https://source-one.example", "https://source-two.example"),
				java.util.Arrays.asList(
						new RagReference(" ABCD ", " Feline calicivirus infection "),
						null,
						new RagReference("ABCD", "Feline calicivirus infection"),
						new RagReference("CFSPH", "Cryptococcosis")),
				new ClinicalSupportTokenUsage(1000L, 500L, 1500L)
		);
	}

	private Diagnostico diagnosticoIa(Consulta consulta) {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setId(50L);
		diagnostico.setDiagnostico("Hipotese");
		diagnostico.setSeveridade("MODERADA");
		diagnostico.setInsightIa("Insight clinico");
		diagnostico.setFontesIaJson("[\"https://source-one.example\",\"https://source-two.example\"]");
		diagnostico.setReferenciasRagJson("[{\"fonte\":\"ABCD\",\"titulo\":\"Feline calicivirus infection\"},"
				+ "{\"fonte\":\"CFSPH\",\"titulo\":\"Cryptococcosis\"}]");
		diagnostico.setConfianca(BigDecimal.valueOf(65));
		diagnostico.setConfirmado("N");
		diagnostico.setValidacaoVet(null);
		diagnostico.setTokensEntrada(1000L);
		diagnostico.setTokensSaida(500L);
		diagnostico.setTokensTotal(1500L);
		diagnostico.setConsulta(consulta);
		return diagnostico;
	}

	private Consulta consulta(String status) {
		Animal animal = new Animal();
		animal.setId(99L);
		animal.setNome("Bilu");
		Veterinario veterinario = new Veterinario();
		veterinario.setId(10L);
		veterinario.setNome("Dra Vera");
		Consulta consulta = new Consulta();
		consulta.setId(6L);
		consulta.setStatus(status);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		return consulta;
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}
}
