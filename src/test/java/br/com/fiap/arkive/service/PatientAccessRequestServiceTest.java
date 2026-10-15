package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.CreatePatientAccessRequest;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.PatientAccess;
import br.com.fiap.arkive.entity.PatientAccessRequest;
import br.com.fiap.arkive.entity.PatientAccessRequestStatus;
import br.com.fiap.arkive.entity.PatientAccessRole;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.PatientAccessRequestRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientAccessRequestServiceTest {

	private static final Clock CLOCK = Clock.fixed(
			Instant.parse("2026-10-14T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
	private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

	private PatientAccessRequestRepository requests;
	private AnimalRepository animals;
	private UsuarioRepository users;
	private AnimalResponsavelRepository relationships;
	private PatientAccessService accesses;
	private ClinicalAccessService clinicalAccess;
	private NotificacaoRecipientService recipients;
	private NotificacaoService notifications;
	private EventoJornadaService events;
	private PatientAccessRequestService service;
	private Animal animal;
	private Usuario requester;
	private Usuario responsible;

	@BeforeEach
	void setUp() {
		requests = mock(PatientAccessRequestRepository.class);
		animals = mock(AnimalRepository.class);
		users = mock(UsuarioRepository.class);
		relationships = mock(AnimalResponsavelRepository.class);
		accesses = mock(PatientAccessService.class);
		clinicalAccess = mock(ClinicalAccessService.class);
		recipients = mock(NotificacaoRecipientService.class);
		notifications = mock(NotificacaoService.class);
		events = mock(EventoJornadaService.class);
		service = new PatientAccessRequestService(requests, animals, users, relationships, accesses,
				clinicalAccess, recipients, notifications, events, CLOCK);
		animal = animal();
		requester = veterinarioUsuario(1L, 10L, "Dra. Fernanda");
		responsible = responsavelUsuario();
		when(users.findUsuarioVeterinarioAtivo(10L)).thenReturn(Optional.of(requester));
		when(users.findById(2L)).thenReturn(Optional.of(responsible));
		when(animals.findByArkivePatientId("ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD"))
				.thenReturn(Optional.of(animal));
		when(animals.buscarParaAtualizarVinculos(50L)).thenReturn(Optional.of(animal));
		when(recipients.responsaveisAtivosVigentes(animal)).thenReturn(List.of(responsible));
		when(relationships.existsVinculoAtivoVigente(50L, 40L, LocalDate.now(CLOCK))).thenReturn(true);
		when(events.criarPayload(any(), any(), any())).thenReturn("{}");
		when(requests.save(any())).thenAnswer(invocation -> {
			PatientAccessRequest request = invocation.getArgument(0);
			if (request.getId() == null) request.setId(91L);
			return request;
		});
	}

	@Test
	void criaPendentePeloArkiveIdNormalizadoSemConcederAcessoNemExporNome() {
		var response = service.criar(
				new CreatePatientAccessRequest("  ark-pet-aaaaa-bbbbb-ccccc-ddddd  "), vetPrincipal());

		assertEquals(91L, response.id());
		assertEquals(PatientAccessRequestStatus.PENDING, response.status());
		assertEquals(NOW, response.requestedAt());
		assertEquals("ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD", response.arkivePatientId());
		assertNull(response.animalNome());
		assertNull(response.resolvedByUserId());
		assertNull(response.resultingAccessId());
		verify(animals).buscarParaAtualizarVinculos(50L);
		verify(accesses, never()).grantOrGetExisting(any(), any(), any(), any());
		verify(notifications).criarParaSolicitacaoAcesso(any(PatientAccessRequest.class), eq(List.of(responsible)));
		verify(events).registrarEvento(eq("SOLICITACAO_ACESSO_CRIADA"), eq("VETERINARIO"),
				eq(null), eq(10L), eq(50L), eq(null), eq("Solicitação de acesso ao paciente criada."), eq("{}"));
	}

	@Test
	void rejeitaPacienteDesconhecidoInativoOuSemResponsavel() {
		when(animals.findByArkivePatientId("ARK-PET-UNKNOWN")).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class,
				() -> service.criar(new CreatePatientAccessRequest("ARK-PET-UNKNOWN"), vetPrincipal()));

		animal.setAtivo("N");
		assertConflict(() -> service.criar(request(), vetPrincipal()));
		animal.setAtivo("S");
		when(recipients.responsaveisAtivosVigentes(animal)).thenReturn(List.of());
		assertConflict(() -> service.criar(request(), vetPrincipal()));

		verify(requests, never()).save(any());
		verify(notifications, never()).criarParaSolicitacaoAcesso(any(), any());
	}

	@Test
	void acessoClinicoExistenteOuPendenteDuplicadoRetornaConflitoSemNotificacao() {
		when(clinicalAccess.podeLerAnimal(any(UsuarioPrincipal.class), eq(animal))).thenReturn(true);
		assertConflict(() -> service.criar(request(), vetPrincipal()));

		when(clinicalAccess.podeLerAnimal(any(UsuarioPrincipal.class), eq(animal))).thenReturn(false);
		when(requests.existsByAnimalIdAndRequesterIdAndStatus(50L, 1L, PatientAccessRequestStatus.PENDING))
				.thenReturn(true);
		assertConflict(() -> service.criar(request(), vetPrincipal()));

		verify(requests, never()).save(any());
		verify(notifications, never()).criarParaSolicitacaoAcesso(any(), any());
	}

	@Test
	void somenteVeterinarioAtivoECoerenteComPrincipalPodeCriar() {
		assertThrows(AccessDeniedException.class, () -> service.criar(request(), responsavelPrincipal()));
		assertThrows(AccessDeniedException.class, () -> service.criar(request(), principal(TipoUsuario.SYSADMIN, 9L, null, null)));
		assertThrows(AccessDeniedException.class,
				() -> service.criar(request(), principal(TipoUsuario.VETERINARIO, 99L, null, 10L)));
		requester.getVeterinario().setAtivo("N");
		assertThrows(AccessDeniedException.class, () -> service.criar(request(), vetPrincipal()));
	}

	@Test
	void veterinariosDiferentesPodemCriarPedidosIndependentes() {
		service.criar(request(), vetPrincipal());
		Usuario outro = veterinarioUsuario(3L, 11L, "Dr. Bruno");
		when(users.findUsuarioVeterinarioAtivo(11L)).thenReturn(Optional.of(outro));
		service.criar(request(), principal(TipoUsuario.VETERINARIO, 3L, null, 11L));

		verify(requests).existsByAnimalIdAndRequesterIdAndStatus(50L, 1L, PatientAccessRequestStatus.PENDING);
		verify(requests).existsByAnimalIdAndRequesterIdAndStatus(50L, 3L, PatientAccessRequestStatus.PENDING);
		verify(requests, times(2)).save(any());
	}

	@Test
	void responsavelAtualAprovaAtomicamenteEReutilizaPatientAccess() {
		PatientAccessRequest request = pendingRequest();
		PatientAccess access = access();
		when(requests.findByIdForUpdate(91L)).thenReturn(Optional.of(request));
		when(accesses.grantOrGetExisting(animal, requester, PatientAccessRole.VETERINARIO, responsible))
				.thenReturn(access);

		var response = service.aprovar(91L, responsavelPrincipal());

		assertEquals(PatientAccessRequestStatus.APPROVED, response.status());
		assertEquals(2L, response.resolvedByUserId());
		assertEquals(70L, response.resultingAccessId());
		assertEquals("Epona", response.animalNome());
		assertEquals(NOW, request.getResolvedAt());
		assertSame(access, request.getResultingAccess());
		verify(requests).findByIdForUpdate(91L);
		verify(animals).buscarParaAtualizarVinculos(50L);
		verify(accesses).grantOrGetExisting(animal, requester, PatientAccessRole.VETERINARIO, responsible);
		verify(animals, never()).save(any());
		verify(relationships, never()).save(any());
		verify(events).registrarEvento(eq("SOLICITACAO_ACESSO_APROVADA"), eq("RESPONSAVEL"),
				eq(40L), eq(10L), eq(50L), eq(null), any(), eq("{}"));
	}

	@Test
	void segundaAprovacaoEhIdempotenteERejeicaoPosteriorConflita() {
		PatientAccessRequest request = pendingRequest();
		request.setStatus(PatientAccessRequestStatus.APPROVED);
		request.setResolvedBy(responsible);
		request.setResolvedAt(NOW);
		request.setResultingAccess(access());
		when(requests.findByIdForUpdate(91L)).thenReturn(Optional.of(request));

		assertEquals(PatientAccessRequestStatus.APPROVED,
				service.aprovar(91L, responsavelPrincipal()).status());
		assertConflict(() -> service.rejeitar(91L, responsavelPrincipal()));

		verify(accesses, never()).grantOrGetExisting(any(), any(), any(), any());
		verify(requests, never()).save(any());
	}

	@Test
	void responsavelAntigoOuNaoRelacionadoNaoPodeResolver() {
		when(requests.findByIdForUpdate(91L)).thenReturn(Optional.of(pendingRequest()));
		when(relationships.existsVinculoAtivoVigente(50L, 40L, LocalDate.now(CLOCK))).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> service.aprovar(91L, responsavelPrincipal()));
		assertThrows(AccessDeniedException.class, () -> service.rejeitar(91L, responsavelPrincipal()));

		verify(accesses, never()).grantOrGetExisting(any(), any(), any(), any());
	}

	@Test
	void solicitanteInativoPermanecePendenteENaoRecebeAcesso() {
		PatientAccessRequest request = pendingRequest();
		requester.setAtivo("N");
		when(requests.findByIdForUpdate(91L)).thenReturn(Optional.of(request));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.aprovar(91L, responsavelPrincipal()));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		assertEquals(PatientAccessRequestStatus.PENDING, request.getStatus());
		assertNull(request.getResolvedAt());
		verify(accesses, never()).grantOrGetExisting(any(), any(), any(), any());
	}

	@Test
	void rejeicaoEhIdempotenteNaoCriaAcessoENovaSolicitacaoFuturaContinuaPermitida() {
		PatientAccessRequest rejected = pendingRequest();
		when(requests.findByIdForUpdate(91L)).thenReturn(Optional.of(rejected));

		assertEquals(PatientAccessRequestStatus.REJECTED,
				service.rejeitar(91L, responsavelPrincipal()).status());
		assertEquals(PatientAccessRequestStatus.REJECTED,
				service.rejeitar(91L, responsavelPrincipal()).status());
		assertConflict(() -> service.aprovar(91L, responsavelPrincipal()));
		verify(accesses, never()).grantOrGetExisting(any(), any(), any(), any());

		when(requests.existsByAnimalIdAndRequesterIdAndStatus(50L, 1L, PatientAccessRequestStatus.PENDING))
				.thenReturn(false);
		service.criar(request(), vetPrincipal());
		verify(requests, times(2)).save(any());
	}

	@Test
	void listasSaoEscopadasPorSolicitanteOuVinculoResponsavelAtual() {
		PageRequest pageable = PageRequest.of(0, 20);
		PatientAccessRequest request = pendingRequest();
		when(requests.findForRequester(1L, PatientAccessRequestStatus.PENDING, pageable))
				.thenReturn(new PageImpl<>(List.of(request), pageable, 1));
		when(requests.findForCurrentResponsible(40L, LocalDate.now(CLOCK), PatientAccessRequestStatus.PENDING, pageable))
				.thenReturn(new PageImpl<>(List.of(request), pageable, 1));

		var vetPage = service.listar(PatientAccessRequestStatus.PENDING, pageable, vetPrincipal());
		var responsiblePage = service.listar(PatientAccessRequestStatus.PENDING, pageable, responsavelPrincipal());

		assertNull(vetPage.getContent().get(0).animalNome());
		assertEquals("Epona", responsiblePage.getContent().get(0).animalNome());
		verify(requests).findForRequester(1L, PatientAccessRequestStatus.PENDING, pageable);
		verify(requests).findForCurrentResponsible(40L, LocalDate.now(CLOCK),
				PatientAccessRequestStatus.PENDING, pageable);
	}

	@Test
	void detalheNaoPodeSerInspecionadoPorVeterinarioOuResponsavelNaoRelacionado() {
		PatientAccessRequest request = pendingRequest();
		when(requests.findDetailedById(91L)).thenReturn(Optional.of(request));
		Usuario outroVet = veterinarioUsuario(3L, 11L, "Dr. Outro");
		when(users.findUsuarioVeterinarioAtivo(11L)).thenReturn(Optional.of(outroVet));

		assertThrows(AccessDeniedException.class,
				() -> service.buscar(91L, principal(TipoUsuario.VETERINARIO, 3L, null, 11L)));
		when(relationships.existsVinculoAtivoVigente(50L, 40L, LocalDate.now(CLOCK))).thenReturn(false);
		assertThrows(AccessDeniedException.class, () -> service.buscar(91L, responsavelPrincipal()));
	}

	@Test
	void solicitanteSoVeNomeDepoisDaAprovacaoEVeNuncaAIdentidadeDoResolvedor() {
		PatientAccessRequest request = pendingRequest();
		request.setStatus(PatientAccessRequestStatus.APPROVED);
		request.setResolvedBy(responsible);
		request.setResolvedAt(NOW);
		request.setResultingAccess(access());
		when(requests.findDetailedById(91L)).thenReturn(Optional.of(request));

		var response = service.buscar(91L, vetPrincipal());

		assertEquals("Epona", response.animalNome());
		assertNull(response.resolvedByUserId());
		assertEquals(70L, response.resultingAccessId());
	}

	@Test
	void aprovacaoDaAcessoClinicoMasNaoAosPrecosDeOutroVeterinario() {
		PatientAccessRequest request = pendingRequest();
		PatientAccess access = access();
		when(requests.findByIdForUpdate(91L)).thenReturn(Optional.of(request));
		when(accesses.grantOrGetExisting(any(), any(), any(), any())).thenReturn(access);
		service.aprovar(91L, responsavelPrincipal());

		Veterinario outro = new Veterinario();
		outro.setId(99L);
		Consulta consultaAnterior = new Consulta();
		consultaAnterior.setVeterinario(outro);

		assertFalse(new FinancialVisibilityService().podeVerCustosConsulta(vetPrincipal(), consultaAnterior));
	}

	private void assertConflict(org.junit.jupiter.api.function.Executable executable) {
		BusinessException exception = assertThrows(BusinessException.class, executable);
		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}

	private CreatePatientAccessRequest request() {
		return new CreatePatientAccessRequest("ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD");
	}

	private PatientAccessRequest pendingRequest() {
		PatientAccessRequest request = new PatientAccessRequest();
		request.setId(91L);
		request.setAnimal(animal);
		request.setRequester(requester);
		request.setStatus(PatientAccessRequestStatus.PENDING);
		request.setRequestedAt(NOW.minusHours(1));
		return request;
	}

	private PatientAccess access() {
		PatientAccess access = new PatientAccess();
		access.setId(70L);
		access.setAnimal(animal);
		access.setUsuario(requester);
		access.setAccessRole(PatientAccessRole.VETERINARIO);
		access.setGrantedBy(responsible);
		access.setGrantedAt(NOW);
		return access;
	}

	private Animal animal() {
		Animal animal = new Animal();
		animal.setId(50L);
		animal.setNome("Epona");
		animal.setAtivo("S");
		animal.assignArkivePatientId("ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD");
		return animal;
	}

	private Usuario veterinarioUsuario(Long usuarioId, Long veterinarioId, String nome) {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		veterinario.setNome(nome);
		veterinario.setCrmv("CRMV" + veterinarioId);
		veterinario.setAtivo("S");
		Usuario usuario = new Usuario();
		usuario.setId(usuarioId);
		usuario.setNome(nome);
		usuario.setTipo(TipoUsuario.VETERINARIO);
		usuario.setAtivo("S");
		usuario.setVeterinario(veterinario);
		return usuario;
	}

	private Usuario responsavelUsuario() {
		Responsavel responsavel = new Responsavel();
		responsavel.setId(40L);
		responsavel.setNome("Responsável Ana");
		responsavel.setAtivo("S");
		Usuario usuario = new Usuario();
		usuario.setId(2L);
		usuario.setNome("Responsável Ana");
		usuario.setTipo(TipoUsuario.RESPONSAVEL);
		usuario.setAtivo("S");
		usuario.setResponsavel(responsavel);
		return usuario;
	}

	private UsuarioPrincipal vetPrincipal() {
		return principal(TipoUsuario.VETERINARIO, 1L, null, 10L);
	}

	private UsuarioPrincipal responsavelPrincipal() {
		return principal(TipoUsuario.RESPONSAVEL, 2L, 40L, null);
	}

	private UsuarioPrincipal principal(TipoUsuario tipo, Long usuarioId, Long responsavelId, Long veterinarioId) {
		return new UsuarioPrincipal(usuarioId, "Usuario", "usuario@arkive.com", "hash", tipo,
				"S", false, responsavelId, veterinarioId, null);
	}
}
