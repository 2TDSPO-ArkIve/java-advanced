package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultaServiceTest {

	private ConsultaRepository consultaRepository;
	private AnimalRepository animalRepository;
	private VeterinarioRepository veterinarioRepository;
	private ClinicaRepository clinicaRepository;
	private EventoJornadaService eventoJornadaService;
	private ClinicalAccessService clinicalAccessService;
	private ConsultaService consultaService;

	@BeforeEach
	void setUp() {
		consultaRepository = mock(ConsultaRepository.class);
		animalRepository = mock(AnimalRepository.class);
		veterinarioRepository = mock(VeterinarioRepository.class);
		clinicaRepository = mock(ClinicaRepository.class);
		eventoJornadaService = mock(EventoJornadaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		consultaService = new ConsultaService(
				consultaRepository,
				animalRepository,
				veterinarioRepository,
				clinicaRepository,
				eventoJornadaService,
				clinicalAccessService
		);
		when(consultaRepository.save(any(Consulta.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(eventoJornadaService.criarPayload(any(), any(), any())).thenReturn("{\"entity\":\"Consulta\"}");
		when(animalRepository.findById(10L)).thenReturn(Optional.of(animal()));
		when(veterinarioRepository.findById(20L)).thenReturn(Optional.of(veterinario()));
		when(clinicaRepository.findById(30L)).thenReturn(Optional.of(clinica()));
	}

	@Test
	void criaConsultaSempreAgendadaQuandoStatusOmitidoOuAg() {
		consultaService.criar(request(null));
		consultaService.criar(request("AG"));

		verify(consultaRepository, times(2)).save(any(Consulta.class));
	}

	@Test
	void rejeitaCriacaoComStatusDiferenteDeAg() {
		assertThrows(BusinessException.class, () -> consultaService.criar(request("EP")));
		assertThrows(BusinessException.class, () -> consultaService.criar(request("FI")));
		verify(consultaRepository, never()).save(any());
	}

	@Test
	void putNaoAlteraStatusDaConsulta() {
		Consulta consulta = consulta("EP");
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));

		assertThrows(BusinessException.class, () -> consultaService.atualizar(1L, request("FI")));

		assertEquals("EP", consulta.getStatus());
		verify(consultaRepository, never()).save(any());
	}

	@Test
	void putMantemEdicaoDeCamposQuandoStatusNaoMuda() {
		Consulta consulta = consulta("EP");
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));
		ConsultaRequest request = new ConsultaRequest(
				LocalDateTime.now().plusDays(1),
				"REMOTA",
				"Retorno",
				"Sintomas atualizados",
				"Observacao atualizada",
				null,
				"Narrativa atualizada",
				"EP",
				10L,
				20L,
				30L
		);

		consultaService.atualizar(1L, request);

		assertEquals("EP", consulta.getStatus());
		assertEquals("REMOTA", consulta.getModalidade());
		assertEquals("Narrativa atualizada", consulta.getTranscricao());
		verify(consultaRepository).save(consulta);
	}

	@Test
	void listagemAutorizadaDeVeterinarioSempreUsaVeterinarioAutenticado() {
		var principal = new br.com.fiap.arkive.security.UsuarioPrincipal(
				1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash",
				br.com.fiap.arkive.entity.TipoUsuario.VETERINARIO, "S", false, null, 20L, null
		);
		when(consultaRepository.buscar(null, 20L, null, null, null, org.springframework.data.domain.Pageable.unpaged()))
				.thenReturn(org.springframework.data.domain.Page.empty());

		consultaService.listarAutorizado(null, null, null, null, null, org.springframework.data.domain.Pageable.unpaged(), principal);

		verify(consultaRepository).buscar(null, 20L, null, null, null, org.springframework.data.domain.Pageable.unpaged());
	}

	@Test
	void listagemAutorizadaDeVeterinarioComFiltroConflitanteRetornaVazia() {
		var principal = new br.com.fiap.arkive.security.UsuarioPrincipal(
				1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash",
				br.com.fiap.arkive.entity.TipoUsuario.VETERINARIO, "S", false, null, 20L, null
		);

		var resultado = consultaService.listarAutorizado(null, 22L, null, null, null, org.springframework.data.domain.Pageable.unpaged(), principal);

		assertEquals(0, resultado.getTotalElements());
		verify(consultaRepository, never()).buscar(null, 22L, null, null, null, org.springframework.data.domain.Pageable.unpaged());
	}

	@Test
	void buscaAutorizadaValidaAcessoAoRecurso() {
		Consulta consulta = consulta("EP");
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));
		var principal = new br.com.fiap.arkive.security.UsuarioPrincipal(
				1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash",
				br.com.fiap.arkive.entity.TipoUsuario.VETERINARIO, "S", false, null, 20L, null
		);

		consultaService.buscarPorIdAutorizado(1L, principal);

		verify(clinicalAccessService).exigirLeituraConsulta(principal, consulta);
	}

	private ConsultaRequest request(String status) {
		return new ConsultaRequest(
				LocalDateTime.now(),
				"PRESENCIAL",
				"Check-up",
				null,
				null,
				null,
				null,
				status,
				10L,
				20L,
				30L
		);
	}

	private Consulta consulta(String status) {
		Consulta consulta = new Consulta();
		consulta.setStatus(status);
		consulta.setAnimal(animal());
		consulta.setVeterinario(veterinario());
		consulta.setClinica(clinica());
		return consulta;
	}

	private Animal animal() {
		Animal animal = new Animal();
		animal.setId(10L);
		animal.setNome("Nina");
		animal.setAtivo("S");
		return animal;
	}

	private Veterinario veterinario() {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(20L);
		veterinario.setNome("Dra Vera");
		veterinario.setAtivo("S");
		return veterinario;
	}

	private Clinica clinica() {
		Clinica clinica = new Clinica();
		clinica.setId(30L);
		clinica.setNome("Clinica Arkive");
		clinica.setAtivo("S");
		return clinica;
	}
}
