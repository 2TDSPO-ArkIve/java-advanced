package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.PrescricaoRequest;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescricaoAuthorizationServiceTest {

	private PrescricaoRepository prescricaoRepository;
	private ConsultaService consultaService;
	private EventoJornadaService eventoJornadaService;
	private ClinicalAccessService clinicalAccessService;
	private PrescricaoService prescricaoService;

	@BeforeEach
	void setUp() {
		prescricaoRepository = mock(PrescricaoRepository.class);
		consultaService = mock(ConsultaService.class);
		eventoJornadaService = mock(EventoJornadaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		prescricaoService = new PrescricaoService(
				prescricaoRepository,
				consultaService,
				eventoJornadaService,
				clinicalAccessService
		);
		when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(eventoJornadaService.criarPayload(any(), any(), any())).thenReturn("{\"entity\":\"Prescricao\"}");
	}

	@Test
	void veterinarioCriaPrescricaoSomenteAposValidarConsultaPropria() {
		Consulta consulta = consulta(10L);
		UsuarioPrincipal principal = principal(TipoUsuario.VETERINARIO, 10L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		prescricaoService.criar(request(1L), principal);

		verify(clinicalAccessService).exigirEscritaPrescricaoVeterinario(eq(principal), any(Prescricao.class));
		verify(prescricaoRepository).save(any(Prescricao.class));
	}

	@Test
	void responsavelNaoCriaPrescricaoVeterinaria() {
		Consulta consulta = consulta(10L);
		UsuarioPrincipal principal = principal(TipoUsuario.RESPONSAVEL, null);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		doThrow(new AccessDeniedException("Operacao clinica permitida apenas ao veterinario responsavel."))
				.when(clinicalAccessService).exigirEscritaPrescricaoVeterinario(eq(principal), any(Prescricao.class));

		assertThrows(AccessDeniedException.class, () -> prescricaoService.criar(request(1L), principal));

		verify(prescricaoRepository, never()).save(any());
	}

	@Test
	void adminClinicaNaoCriaPrescricaoVeterinaria() {
		Consulta consulta = consulta(10L);
		UsuarioPrincipal principal = principal(TipoUsuario.ADMIN_CLINICA, null);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		doThrow(new AccessDeniedException("Operacao clinica permitida apenas ao veterinario responsavel."))
				.when(clinicalAccessService).exigirEscritaPrescricaoVeterinario(eq(principal), any(Prescricao.class));

		assertThrows(AccessDeniedException.class, () -> prescricaoService.criar(request(1L), principal));

		verify(prescricaoRepository, never()).save(any());
	}

	@Test
	void atualizacaoDePrescricaoValidaConsultaResultante() {
		Prescricao prescricao = prescricao(consulta(10L));
		UsuarioPrincipal principal = principal(TipoUsuario.VETERINARIO, 10L);
		when(prescricaoRepository.findById(9L)).thenReturn(Optional.of(prescricao));
		when(consultaService.buscarEntidade(1L)).thenReturn(prescricao.getConsulta());

		prescricaoService.atualizar(9L, request(1L), principal);

		verify(clinicalAccessService).exigirEscritaPrescricaoVeterinario(principal, prescricao);
		verify(prescricaoRepository).save(prescricao);
	}

	private PrescricaoRequest request(Long consultaId) {
		return new PrescricaoRequest("Medicamento", "1 comprimido", "12/12h", "ORAL", LocalDate.now(), null, "Com alimento", consultaId);
	}

	private Prescricao prescricao(Consulta consulta) {
		Prescricao prescricao = new Prescricao();
		prescricao.setId(9L);
		prescricao.setConsulta(consulta);
		return prescricao;
	}

	private Consulta consulta(Long veterinarioId) {
		Animal animal = new Animal();
		animal.setId(50L);
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		Consulta consulta = new Consulta();
		consulta.setId(1L);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		return consulta;
	}

	private UsuarioPrincipal principal(TipoUsuario tipo, Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "$2a$10$hash", tipo, "S", false, null, veterinarioId, null);
	}

}
