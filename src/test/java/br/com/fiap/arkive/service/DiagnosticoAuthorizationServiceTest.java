package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.DiagnosticoRequest;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.DoencaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagnosticoAuthorizationServiceTest {

	private DiagnosticoRepository diagnosticoRepository;
	private ConsultaService consultaService;
	private ClinicalAccessService clinicalAccessService;
	private DiagnosticoService diagnosticoService;

	@BeforeEach
	void setUp() {
		diagnosticoRepository = mock(DiagnosticoRepository.class);
		consultaService = mock(ConsultaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		diagnosticoService = new DiagnosticoService(
				diagnosticoRepository,
				consultaService,
				mock(DoencaRepository.class),
				clinicalAccessService
		);
		when(diagnosticoRepository.save(any(Diagnostico.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void veterinarioCriaDiagnosticoSomenteAposValidarConsulta() {
		Consulta consulta = consulta();
		UsuarioPrincipal principal = veterinario();
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		diagnosticoService.criar(request(), principal);

		verify(clinicalAccessService).exigirEscritaDiagnosticoVeterinario(eq(principal), any(Diagnostico.class));
		verify(diagnosticoRepository).save(any(Diagnostico.class));
	}

	@Test
	void diagnosticoGenericoNaoAceitaCamposControladosPeloServidor() {
		DiagnosticoRequest request = new DiagnosticoRequest("Diagnostico", "LEVE", "S", "Insight", null, "S", 1L, null);

		BusinessException exception = assertThrows(BusinessException.class, () -> diagnosticoService.criar(request, veterinario()));

		assertEquals("Campos de IA, confianca, confirmacao e validacao veterinaria sao controlados pelo servidor.", exception.getMessage());
	}

	@Test
	void atualizacaoGenericaNaoAceitaValidacaoVeterinaria() {
		Diagnostico diagnostico = diagnostico(consulta());
		when(diagnosticoRepository.findById(5L)).thenReturn(Optional.of(diagnostico));
		DiagnosticoRequest request = new DiagnosticoRequest("Diagnostico", "LEVE", null, null, null, "S", 1L, null);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> diagnosticoService.atualizar(5L, request, veterinario()));

		assertEquals("Campos de IA, confianca, confirmacao e validacao veterinaria sao controlados pelo servidor.", exception.getMessage());
	}

	@Test
	void diagnosticoGenericoCriadoPeloClientePermaneceNaoConfirmado() {
		Consulta consulta = consulta();
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		diagnosticoService.criar(request(), veterinario());

		verify(diagnosticoRepository).save(org.mockito.ArgumentMatchers.argThat(diagnostico ->
				"N".equals(diagnostico.getConfirmado())
						&& diagnostico.getValidacaoVet() == null
						&& diagnostico.getInsightIa() == null
						&& diagnostico.getConfianca() == null
		));
	}

	@Test
	void criaSuporteClinicoIaComoProvisorioNaoConfirmado() {
		Consulta consulta = consulta();

		Diagnostico diagnostico = diagnosticoService.criarSuporteClinicoIa(consulta, "Hipotese", "MODERADA", "Insight", 65);

		assertEquals("Hipotese", diagnostico.getDiagnostico());
		assertEquals("MODERADA", diagnostico.getSeveridade());
		assertEquals("Insight", diagnostico.getInsightIa());
		assertEquals("65", diagnostico.getConfianca().toPlainString());
		assertNull(diagnostico.getFontesIaJson());
		assertEquals("N", diagnostico.getConfirmado());
		assertNull(diagnostico.getValidacaoVet());
		assertEquals(consulta, diagnostico.getConsulta());
	}

	@Test
	void diagnosticoConfirmadoPeloVeterinarioNaoEhAvaliacaoDeInsightIa() {
		Consulta consulta = consulta();

		Diagnostico diagnostico = diagnosticoService.criarConfirmadoPeloVeterinario(
				consulta, "Diagnostico final", "LEVE", null);

		assertEquals("S", diagnostico.getConfirmado());
		assertNull(diagnostico.getValidacaoVet());
		assertNull(diagnostico.getInsightIa());
		assertNull(diagnostico.getConfianca());
	}

	@Test
	void aceitaSeveridadeAusenteSemConfundirComNulo() {
		Consulta consulta = consulta();

		Diagnostico confirmado = diagnosticoService.criarConfirmadoPeloVeterinario(
				consulta, "Nenhuma doenca identificada", "AUSENTE", null);
		Diagnostico suporte = diagnosticoService.criarSuporteClinicoIa(
				consulta, "Sem achados relevantes", "AUSENTE", "Manter acompanhamento", 80);

		assertEquals("AUSENTE", confirmado.getSeveridade());
		assertEquals("AUSENTE", suporte.getSeveridade());
	}

	@Test
	void persisteTokensDoMotorERejeitaValoresNegativos() {
		Consulta consulta = consulta();

		Diagnostico suporte = diagnosticoService.criarSuporteClinicoIa(
				consulta, "Hipotese", "MODERADA", "Insight", 65,
				null, null, 1000L, 500L, 1500L);

		assertEquals(1000L, suporte.getTokensEntrada());
		assertEquals(500L, suporte.getTokensSaida());
		assertEquals(1500L, suporte.getTokensTotal());
		assertThrows(BusinessException.class, () -> diagnosticoService.criarSuporteClinicoIa(
				consulta, "Hipotese", "MODERADA", "Insight", 65,
				null, null, -1L, 1L, 0L));
	}

	@Test
	void mantemSeveridadesAnterioresERejeitaValorDesconhecido() {
		Consulta consulta = consulta();

		assertEquals("LEVE", diagnosticoService.criarConfirmadoPeloVeterinario(consulta, "Leve", "LEVE", null).getSeveridade());
		assertEquals("MODERADA", diagnosticoService.criarConfirmadoPeloVeterinario(consulta, "Moderada", "MODERADA", null).getSeveridade());
		assertEquals("GRAVE", diagnosticoService.criarConfirmadoPeloVeterinario(consulta, "Grave", "GRAVE", null).getSeveridade());
		BusinessException exception = assertThrows(BusinessException.class,
				() -> diagnosticoService.criarConfirmadoPeloVeterinario(consulta, "Invalida", "CRITICA", null));
		assertEquals("Severidade deve ser AUSENTE, LEVE, MODERADA ou GRAVE.", exception.getMessage());
	}

	@Test
	void criaSuporteClinicoIaPersisteJsonDeFontesQuandoInformado() {
		Consulta consulta = consulta();

		Diagnostico diagnostico = diagnosticoService.criarSuporteClinicoIa(
				consulta,
				"Hipotese",
				"MODERADA",
				"Insight",
				65,
				"[\"https://source-one.example\"]"
		);

		assertEquals("[\"https://source-one.example\"]", diagnostico.getFontesIaJson());
	}

	@Test
	void atualizacaoDeDiagnosticoValidaConsultaResultante() {
		Diagnostico diagnostico = diagnostico(consulta());
		UsuarioPrincipal principal = veterinario();
		when(diagnosticoRepository.findById(5L)).thenReturn(Optional.of(diagnostico));
		when(consultaService.buscarEntidade(1L)).thenReturn(diagnostico.getConsulta());

		diagnosticoService.atualizar(5L, request(), principal);

		verify(clinicalAccessService).exigirEscritaDiagnosticoVeterinario(principal, diagnostico);
		verify(diagnosticoRepository).save(diagnostico);
	}

	@Test
	void leituraPorIdValidaEscopoDaConsulta() {
		Diagnostico diagnostico = diagnostico(consulta());
		UsuarioPrincipal principal = veterinario();
		when(diagnosticoRepository.findById(5L)).thenReturn(Optional.of(diagnostico));

		diagnosticoService.buscarPorIdAutorizado(5L, principal);

		verify(clinicalAccessService).exigirLeituraDiagnostico(principal, diagnostico);
	}

	@Test
	void listagemDeVeterinarioUsaConsultaDoVeterinarioAutenticado() {
		UsuarioPrincipal principal = veterinario();
		when(diagnosticoRepository.buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
				.thenReturn(Page.empty());

		diagnosticoService.listarAutorizado(null, null, null, null, Pageable.unpaged(), principal);

		verify(diagnosticoRepository).buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
	}

	private DiagnosticoRequest request() {
		return new DiagnosticoRequest("Diagnostico", "LEVE", null, null, null, null, 1L, null);
	}

	private Diagnostico diagnostico(Consulta consulta) {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setId(5L);
		diagnostico.setDiagnostico("Diagnostico");
		diagnostico.setConsulta(consulta);
		return diagnostico;
	}

	private Consulta consulta() {
		Animal animal = new Animal();
		animal.setId(50L);
		Veterinario veterinario = new Veterinario();
		veterinario.setId(10L);
		Consulta consulta = new Consulta();
		consulta.setId(1L);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		return consulta;
	}

	private UsuarioPrincipal veterinario() {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
	}

}
