package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.NotificacaoResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Notificacao;
import br.com.fiap.arkive.entity.PatientAccessRequest;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoNotificacao;
import br.com.fiap.arkive.entity.TipoRecursoNotificacao;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.event.NotificacoesCriadasEvent;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.NotificacaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoServiceTest {

	private NotificacaoRepository repository;
	private NotificacaoRecipientService recipientService;
	private ApplicationEventPublisher publisher;
	private NotificacaoService service;

	@BeforeEach
	void setUp() {
		repository = mock(NotificacaoRepository.class);
		recipientService = mock(NotificacaoRecipientService.class);
		publisher = mock(ApplicationEventPublisher.class);
		service = new NotificacaoService(repository, recipientService, publisher);
	}

	@Test
	void criaUmaNotificacaoPorDestinatarioComConteudoClinicoMinimo() {
		Consulta consulta = consulta();
		Usuario primeiro = usuario(10L, "S");
		Usuario segundo = usuario(11L, "N");
		when(recipientService.responsaveisAtivosVigentes(consulta.getAnimal()))
				.thenReturn(List.of(primeiro, segundo));
		when(repository.findByUsuarioIdAndChaveEvento(any(), any())).thenReturn(Optional.empty());
		AtomicLong ids = new AtomicLong(100);
		when(repository.save(any())).thenAnswer(invocation -> {
			Notificacao notificacao = invocation.getArgument(0);
			notificacao.setId(ids.getAndIncrement());
			return notificacao;
		});

		List<Notificacao> criadas = service.criarParaConsultaFinalizada(consulta);

		assertEquals(2, criadas.size());
		for (Notificacao notificacao : criadas) {
			assertEquals(TipoNotificacao.CONSULTA_FINALIZADA, notificacao.getTipo());
			assertEquals("Atualização no ArkIve", notificacao.getTitulo());
			assertEquals("Há uma nova atualização no acompanhamento de Rex.", notificacao.getMensagem());
			assertEquals(consulta.getAnimal(), notificacao.getAnimal());
			assertEquals(TipoRecursoNotificacao.CONSULTA, notificacao.getTipoRecurso());
			assertEquals(77L, notificacao.getRecursoId());
			assertEquals("CONSULTA_FINALIZADA:77", notificacao.getChaveEvento());
		}
		ArgumentCaptor<NotificacoesCriadasEvent> event = ArgumentCaptor.forClass(NotificacoesCriadasEvent.class);
		verify(publisher).publishEvent(event.capture());
		assertEquals(List.of(100L, 101L), event.getValue().notificacaoIds());
	}

	@Test
	void consultaAgendadaNotificaCadaResponsavelComArkiveIdERecursoDaConsulta() {
		Consulta consulta = consulta();
		Usuario primeiro = usuario(10L, "S");
		Usuario segundo = usuario(11L, "N");
		when(recipientService.responsaveisAtivosVigentes(consulta.getAnimal()))
				.thenReturn(List.of(primeiro, segundo));
		when(repository.findByUsuarioIdAndChaveEvento(any(), any())).thenReturn(Optional.empty());
		AtomicLong ids = new AtomicLong(200);
		when(repository.save(any())).thenAnswer(invocation -> {
			Notificacao notificacao = invocation.getArgument(0);
			notificacao.setId(ids.getAndIncrement());
			return notificacao;
		});

		List<Notificacao> criadas = service.criarParaConsultaAgendada(consulta);

		assertEquals(2, criadas.size());
		for (Notificacao notificacao : criadas) {
			assertEquals(TipoNotificacao.CONSULTA_AGENDADA, notificacao.getTipo());
			assertEquals("Consulta agendada", notificacao.getTitulo());
			assertEquals("Uma consulta foi agendada para Rex (ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD).",
					notificacao.getMensagem());
			assertEquals(consulta.getAnimal(), notificacao.getAnimal());
			assertEquals(TipoRecursoNotificacao.CONSULTA, notificacao.getTipoRecurso());
			assertEquals(77L, notificacao.getRecursoId());
			assertEquals("CONSULTA_AGENDADA:77", notificacao.getChaveEvento());
		}
		ArgumentCaptor<NotificacoesCriadasEvent> event = ArgumentCaptor.forClass(NotificacoesCriadasEvent.class);
		verify(publisher).publishEvent(event.capture());
		assertEquals(List.of(200L, 201L), event.getValue().notificacaoIds());
	}

	@Test
	void solicitacaoAcessoNotificaResponsaveisComIdentidadeClinicaERecursoDaSolicitacao() {
		PatientAccessRequest solicitacao = solicitacaoAcesso();
		Usuario primeiro = usuario(10L, "S");
		Usuario segundo = usuario(11L, "N");
		when(repository.findByUsuarioIdAndChaveEvento(any(), any())).thenReturn(Optional.empty());
		AtomicLong ids = new AtomicLong(250);
		when(repository.save(any())).thenAnswer(invocation -> {
			Notificacao notificacao = invocation.getArgument(0);
			notificacao.setId(ids.getAndIncrement());
			return notificacao;
		});

		List<Notificacao> criadas = service.criarParaSolicitacaoAcesso(
				solicitacao, List.of(primeiro, segundo));

		assertEquals(2, criadas.size());
		for (Notificacao notificacao : criadas) {
			assertEquals(TipoNotificacao.SOLICITACAO_ACESSO, notificacao.getTipo());
			assertEquals("Solicitação de acesso", notificacao.getTitulo());
			assertEquals("Fernanda Vieira (CRMV 12345) solicitou acesso a Rex "
					+ "(ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD).", notificacao.getMensagem());
			assertEquals(solicitacao.getAnimal(), notificacao.getAnimal());
			assertEquals(TipoRecursoNotificacao.SOLICITACAO_ACESSO, notificacao.getTipoRecurso());
			assertEquals(91L, notificacao.getRecursoId());
			assertEquals("SOLICITACAO_ACESSO:91", notificacao.getChaveEvento());
		}
		ArgumentCaptor<NotificacoesCriadasEvent> event = ArgumentCaptor.forClass(NotificacoesCriadasEvent.class);
		verify(publisher).publishEvent(event.capture());
		assertEquals(List.of(250L, 251L), event.getValue().notificacaoIds());
	}

	@Test
	void solicitacaoAcessoDuplicadaNaoCriaOutraNotificacaoNemPublicaPush() {
		PatientAccessRequest solicitacao = solicitacaoAcesso();
		Usuario usuario = usuario(10L, "S");
		when(repository.findByUsuarioIdAndChaveEvento(10L, "SOLICITACAO_ACESSO:91"))
				.thenReturn(Optional.of(new Notificacao()));

		assertTrue(service.criarParaSolicitacaoAcesso(solicitacao, List.of(usuario)).isEmpty());

		verify(repository, never()).save(any());
		verify(publisher, never()).publishEvent(any());
	}

	@Test
	void lembreteEhIdempotentePorHorarioEReagendamentoGeraNovaIdentidade() {
		Consulta consulta = consulta();
		consulta.setDataHora(LocalDateTime.of(2026, 10, 20, 14, 30));
		Usuario veterinario = usuarioVeterinario(30L, 9L);
		Map<String, Notificacao> persistidas = new LinkedHashMap<>();
		when(repository.findByUsuarioIdAndChaveEvento(eq(30L), any())).thenAnswer(invocation -> {
			String chaveEvento = invocation.getArgument(1, String.class);
			return Optional.ofNullable(persistidas.get(chaveEvento));
		});
		AtomicLong ids = new AtomicLong(300);
		when(repository.save(any())).thenAnswer(invocation -> {
			Notificacao notificacao = invocation.getArgument(0);
			notificacao.setId(ids.getAndIncrement());
			persistidas.put(notificacao.getChaveEvento(), notificacao);
			return notificacao;
		});

		assertTrue(service.criarLembreteConsulta(consulta, veterinario).isPresent());
		assertTrue(service.criarLembreteConsulta(consulta, veterinario).isEmpty());
		consulta.setDataHora(LocalDateTime.of(2026, 10, 21, 9, 15));
		assertTrue(service.criarLembreteConsulta(consulta, veterinario).isPresent());

		assertEquals(2, persistidas.size());
		Notificacao primeira = persistidas.get("LEMBRETE_CONSULTA:77:2026-10-20T14:30");
		assertNotNull(primeira);
		assertEquals(TipoNotificacao.LEMBRETE_CONSULTA, primeira.getTipo());
		assertEquals("Lembrete de consulta", primeira.getTitulo());
		assertEquals("Consulta de Rex (ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD) em breve.",
				primeira.getMensagem());
		assertEquals(veterinario, primeira.getUsuario());
		assertEquals(TipoRecursoNotificacao.CONSULTA, primeira.getTipoRecurso());
		assertEquals(77L, primeira.getRecursoId());
		verify(repository, times(2)).save(any(Notificacao.class));
	}

	@Test
	void eventoJaPersistidoNaoDuplicaEDestinatarioSemNovaLinhaNaoPublicaPush() {
		Consulta consulta = consulta();
		Usuario usuario = usuario(10L, "S");
		when(recipientService.responsaveisAtivosVigentes(consulta.getAnimal())).thenReturn(List.of(usuario));
		when(repository.findByUsuarioIdAndChaveEvento(10L, "CONSULTA_FINALIZADA:77"))
				.thenReturn(Optional.of(new Notificacao()));

		assertTrue(service.criarParaConsultaFinalizada(consulta).isEmpty());

		verify(repository, never()).save(any());
		verify(publisher, never()).publishEvent(any());
	}

	@Test
	void consultaAgendadaDuplicadaNaoCriaOutraLinhaParaMesmoUsuario() {
		Consulta consulta = consulta();
		Usuario usuario = usuario(10L, "S");
		when(recipientService.responsaveisAtivosVigentes(consulta.getAnimal())).thenReturn(List.of(usuario));
		when(repository.findByUsuarioIdAndChaveEvento(10L, "CONSULTA_AGENDADA:77"))
				.thenReturn(Optional.of(new Notificacao()));

		assertTrue(service.criarParaConsultaAgendada(consulta).isEmpty());

		verify(repository, never()).save(any());
		verify(publisher, never()).publishEvent(any());
	}

	@Test
	void consultaSemResponsavelAtualNaoCriaNotificacaoOrfa() {
		Consulta consulta = consulta();
		when(recipientService.responsaveisAtivosVigentes(consulta.getAnimal())).thenReturn(List.of());

		assertTrue(service.criarParaConsultaAgendada(consulta).isEmpty());

		verify(repository, never()).save(any());
		verify(publisher, never()).publishEvent(any());
	}

	@Test
	void preferenciaNaoNaoImpedePersistenciaDaCaixaDeEntrada() {
		Consulta consulta = consulta();
		Usuario usuario = usuario(10L, "N");
		when(recipientService.responsaveisAtivosVigentes(consulta.getAnimal())).thenReturn(List.of(usuario));
		when(repository.findByUsuarioIdAndChaveEvento(any(), any())).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(invocation -> {
			Notificacao notificacao = invocation.getArgument(0);
			notificacao.setId(1L);
			return notificacao;
		});

		List<Notificacao> criadas = service.criarParaConsultaFinalizada(consulta);

		assertEquals(1, criadas.size());
		assertEquals("N", criadas.get(0).getUsuario().getResponsavel().getNotificacao());
	}

	@Test
	void listaEContagemSempreUsamUsuarioDoPrincipal() {
		UsuarioPrincipal principal = principal(42L);
		Notificacao notificacao = notificacao(1L, usuario(42L, "S"));
		PageRequest pageable = PageRequest.of(0, 20);
		when(repository.findByUsuarioIdOrderByDataCriacaoDescIdDesc(42L, pageable))
				.thenReturn(new PageImpl<>(List.of(notificacao)));
		when(repository.findByUsuarioIdAndDataLeituraIsNullOrderByDataCriacaoDescIdDesc(42L, pageable))
				.thenReturn(new PageImpl<>(List.of(notificacao)));
		when(repository.countByUsuarioIdAndDataLeituraIsNull(42L)).thenReturn(1L);

		assertEquals(1, service.listar(false, pageable, principal).getTotalElements());
		assertEquals(1, service.listar(true, pageable, principal).getTotalElements());
		assertEquals(1L, service.contarNaoLidas(principal).count());
	}

	@Test
	void marcarComoLidaEhIdempotenteEPreservaPrimeiroInstante() {
		UsuarioPrincipal principal = principal(42L);
		Notificacao notificacao = notificacao(1L, usuario(42L, "S"));
		when(repository.findByIdAndUsuarioId(1L, 42L)).thenReturn(Optional.of(notificacao));

		NotificacaoResponse primeira = service.marcarComoLida(1L, principal);
		NotificacaoResponse segunda = service.marcarComoLida(1L, principal);

		assertNotNull(primeira.readAt());
		assertEquals(primeira.readAt(), segunda.readAt());
		verify(repository, times(1)).save(notificacao);
	}

	@Test
	void usuarioNaoPodeMarcarNotificacaoDeOutraConta() {
		when(repository.findByIdAndUsuarioId(9L, 42L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> service.marcarComoLida(9L, principal(42L)));
		verify(repository, never()).save(any());
	}

	@Test
	void marcarTodasAtualizaSomenteEscopoDoPrincipal() {
		when(repository.marcarTodasComoLidas(eq(42L), any(LocalDateTime.class))).thenReturn(3);

		assertEquals(3, service.marcarTodasComoLidas(principal(42L)).updated());
		verify(repository).marcarTodasComoLidas(eq(42L), any(LocalDateTime.class));
	}

	private Consulta consulta() {
		Animal animal = new Animal();
		animal.setId(5L);
		animal.setNome("Rex");
		animal.assignArkivePatientId("ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD");
		Consulta consulta = new Consulta();
		consulta.setId(77L);
		consulta.setAnimal(animal);
		return consulta;
	}

	private PatientAccessRequest solicitacaoAcesso() {
		Consulta consulta = consulta();
		Veterinario veterinario = new Veterinario();
		veterinario.setId(9L);
		veterinario.setNome("Fernanda Vieira");
		veterinario.setCrmv("12345");
		Usuario solicitante = usuarioVeterinario(30L, 9L);
		solicitante.setVeterinario(veterinario);
		PatientAccessRequest solicitacao = new PatientAccessRequest();
		solicitacao.setId(91L);
		solicitacao.setAnimal(consulta.getAnimal());
		solicitacao.setRequester(solicitante);
		return solicitacao;
	}

	private Usuario usuario(Long id, String notificacaoPreferencia) {
		Responsavel responsavel = new Responsavel();
		responsavel.setNotificacao(notificacaoPreferencia);
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setTipo(TipoUsuario.RESPONSAVEL);
		usuario.setAtivo("S");
		usuario.setResponsavel(responsavel);
		return usuario;
	}

	private Usuario usuarioVeterinario(Long usuarioId, Long veterinarioId) {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		Usuario usuario = new Usuario();
		usuario.setId(usuarioId);
		usuario.setTipo(TipoUsuario.VETERINARIO);
		usuario.setAtivo("S");
		usuario.setVeterinario(veterinario);
		return usuario;
	}

	private Notificacao notificacao(Long id, Usuario usuario) {
		Notificacao notificacao = new Notificacao();
		notificacao.setId(id);
		notificacao.setUsuario(usuario);
		notificacao.setTipo(TipoNotificacao.CONSULTA_FINALIZADA);
		notificacao.setTitulo("Atualização no ArkIve");
		notificacao.setMensagem("Há uma nova atualização no acompanhamento de Rex.");
		notificacao.setAnimal(consulta().getAnimal());
		notificacao.setTipoRecurso(TipoRecursoNotificacao.CONSULTA);
		notificacao.setRecursoId(77L);
		assertNull(notificacao.getDataLeitura());
		return notificacao;
	}

	private UsuarioPrincipal principal(Long usuarioId) {
		return new UsuarioPrincipal(usuarioId, "Responsável", "responsavel@example.test", "hash",
				TipoUsuario.RESPONSAVEL, "S", false, 1L, null, null);
	}
}
