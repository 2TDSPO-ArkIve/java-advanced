package br.com.fiap.arkive.service;

import br.com.fiap.arkive.config.ConsultaReminderProperties;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Notificacao;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.ConsultaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultaReminderServiceTest {

	private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 20, 14, 0);

	private ConsultaRepository consultaRepository;
	private NotificacaoRecipientService recipientService;
	private NotificacaoService notificacaoService;
	private ConsultaReminderService service;

	@BeforeEach
	void setUp() {
		consultaRepository = mock(ConsultaRepository.class);
		recipientService = mock(NotificacaoRecipientService.class);
		notificacaoService = mock(NotificacaoService.class);
		ConsultaReminderProperties properties = new ConsultaReminderProperties();
		Clock clock = Clock.fixed(Instant.parse("2026-10-20T17:00:00Z"), ZoneId.of("America/Sao_Paulo"));
		service = new ConsultaReminderService(
				consultaRepository, recipientService, notificacaoService, properties, clock);
	}

	@Test
	void usaRelogioDeNegocioEJanelaPadraoDeTrintaMinutos() {
		Consulta consulta = consulta(1L, AGORA.plusMinutes(30));
		Usuario veterinario = usuarioVeterinario(20L, consulta.getVeterinario());
		when(consultaRepository.buscarAgendadasParaLembrete(AGORA, AGORA.plusMinutes(30)))
				.thenReturn(List.of(consulta));
		when(recipientService.veterinarioAtivo(consulta.getVeterinario()))
				.thenReturn(Optional.of(veterinario));
		when(notificacaoService.criarLembreteConsulta(consulta, veterinario))
				.thenReturn(Optional.of(new Notificacao()));

		assertEquals(1, service.processarLembretes());

		verify(consultaRepository).buscarAgendadasParaLembrete(AGORA, AGORA.plusMinutes(30));
		verify(notificacaoService).criarLembreteConsulta(consulta, veterinario);
	}

	@Test
	void contaSomenteNotificacoesNovasQuandoExecucaoEhRepetida() {
		Consulta consulta = consulta(1L, AGORA.plusMinutes(10));
		Usuario veterinario = usuarioVeterinario(20L, consulta.getVeterinario());
		when(consultaRepository.buscarAgendadasParaLembrete(
				any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(consulta));
		when(recipientService.veterinarioAtivo(consulta.getVeterinario()))
				.thenReturn(Optional.of(veterinario));
		when(notificacaoService.criarLembreteConsulta(consulta, veterinario))
				.thenReturn(Optional.of(new Notificacao()))
				.thenReturn(Optional.empty());

		assertEquals(1, service.processarLembretes());
		assertEquals(0, service.processarLembretes());
	}

	@Test
	void contaVeterinariaInativaEhIgnoradaSemImpedirOutraConsulta() {
		Consulta semContaAtiva = consulta(1L, AGORA.plusMinutes(10));
		Consulta valida = consulta(2L, AGORA.plusMinutes(20));
		Usuario veterinario = usuarioVeterinario(21L, valida.getVeterinario());
		when(consultaRepository.buscarAgendadasParaLembrete(
				any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(List.of(semContaAtiva, valida));
		when(recipientService.veterinarioAtivo(semContaAtiva.getVeterinario()))
				.thenReturn(Optional.empty());
		when(recipientService.veterinarioAtivo(valida.getVeterinario()))
				.thenReturn(Optional.of(veterinario));
		when(notificacaoService.criarLembreteConsulta(valida, veterinario))
				.thenReturn(Optional.of(new Notificacao()));

		assertEquals(1, service.processarLembretes());

		verify(notificacaoService, never()).criarLembreteConsulta(semContaAtiva, veterinario);
		verify(notificacaoService).criarLembreteConsulta(valida, veterinario);
	}

	@Test
	void consultaMalformadaEhIsoladaSemCriarDestinatarioAlternativo() {
		Consulta malformada = consulta(1L, AGORA.plusMinutes(5));
		Animal animalSemArkiveId = new Animal();
		animalSemArkiveId.setId(101L);
		animalSemArkiveId.setNome("Epona");
		malformada.setAnimal(animalSemArkiveId);
		Consulta valida = consulta(2L, AGORA.plusMinutes(10));
		Usuario veterinario = usuarioVeterinario(22L, valida.getVeterinario());
		when(consultaRepository.buscarAgendadasParaLembrete(
				any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(List.of(malformada, valida));
		when(recipientService.veterinarioAtivo(valida.getVeterinario()))
				.thenReturn(Optional.of(veterinario));
		when(notificacaoService.criarLembreteConsulta(valida, veterinario))
				.thenReturn(Optional.of(new Notificacao()));

		assertEquals(1, service.processarLembretes());

		verify(recipientService, never()).veterinarioAtivo(malformada.getVeterinario());
		verify(notificacaoService).criarLembreteConsulta(valida, veterinario);
	}

	private Consulta consulta(Long id, LocalDateTime dataHora) {
		Animal animal = new Animal();
		animal.setId(100L + id);
		animal.setNome("Epona");
		animal.assignArkivePatientId("ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD-" + id);
		Veterinario veterinario = new Veterinario();
		veterinario.setId(200L + id);
		Consulta consulta = new Consulta();
		consulta.setId(id);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setStatus("AG");
		consulta.setDataHora(dataHora);
		return consulta;
	}

	private Usuario usuarioVeterinario(Long id, Veterinario veterinario) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setTipo(TipoUsuario.VETERINARIO);
		usuario.setAtivo("S");
		usuario.setVeterinario(veterinario);
		return usuario;
	}
}
