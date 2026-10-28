package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.CreatePatientAccessRequest;
import br.com.fiap.arkive.dto.response.PatientAccessRequestResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.PatientAccess;
import br.com.fiap.arkive.entity.PatientAccessRequest;
import br.com.fiap.arkive.entity.PatientAccessRequestStatus;
import br.com.fiap.arkive.entity.PatientAccessRole;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.PatientAccessRequestRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Profile("!local-nodb")
public class PatientAccessRequestService {

	private final PatientAccessRequestRepository requestRepository;
	private final AnimalRepository animalRepository;
	private final UsuarioRepository usuarioRepository;
	private final AnimalResponsavelRepository relationshipRepository;
	private final PatientAccessService patientAccessService;
	private final ClinicalAccessService clinicalAccessService;
	private final NotificacaoRecipientService recipientService;
	private final NotificacaoService notificacaoService;
	private final EventoJornadaService eventoJornadaService;
	private final Clock clock;

	public PatientAccessRequestService(
			PatientAccessRequestRepository requestRepository,
			AnimalRepository animalRepository,
			UsuarioRepository usuarioRepository,
			AnimalResponsavelRepository relationshipRepository,
			PatientAccessService patientAccessService,
			ClinicalAccessService clinicalAccessService,
			NotificacaoRecipientService recipientService,
			NotificacaoService notificacaoService,
			EventoJornadaService eventoJornadaService,
			@Qualifier("businessClock") Clock clock
	) {
		this.requestRepository = requestRepository;
		this.animalRepository = animalRepository;
		this.usuarioRepository = usuarioRepository;
		this.relationshipRepository = relationshipRepository;
		this.patientAccessService = patientAccessService;
		this.clinicalAccessService = clinicalAccessService;
		this.recipientService = recipientService;
		this.notificacaoService = notificacaoService;
		this.eventoJornadaService = eventoJornadaService;
		this.clock = clock;
	}

	@Transactional
	public PatientAccessRequestResponse criar(
			CreatePatientAccessRequest request,
			UsuarioPrincipal principal
	) {
		Usuario requester = exigirSolicitanteVeterinario(principal);
		String arkivePatientId = normalizarArkivePatientId(request.arkivePatientId());
		Animal localizado = animalRepository.findByArkivePatientId(arkivePatientId)
				.orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado para o ArkIve ID informado."));
		Animal animal = animalRepository.buscarParaAtualizarVinculos(localizado.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado para o ArkIve ID informado."));

		if (!"S".equals(animal.getAtivo())) {
			throw new BusinessException("Não é possível solicitar acesso a um paciente inativo.", HttpStatus.CONFLICT);
		}
		if (clinicalAccessService.podeLerAnimal(principal, animal)) {
			throw new BusinessException("Você já possui acesso a este paciente.", HttpStatus.CONFLICT);
		}
		if (requestRepository.existsByAnimalIdAndRequesterIdAndStatus(
				animal.getId(), requester.getId(), PatientAccessRequestStatus.PENDING)) {
			throw new BusinessException(
					"Já existe uma solicitação de acesso pendente para este paciente.", HttpStatus.CONFLICT);
		}

		List<Usuario> destinatarios = recipientService.responsaveisAtivosVigentes(animal);
		if (destinatarios.isEmpty()) {
			throw new BusinessException(
					"Este paciente ainda não possui um responsável apto a aprovar o acesso.", HttpStatus.CONFLICT);
		}

		PatientAccessRequest solicitacao = new PatientAccessRequest();
		solicitacao.setAnimal(animal);
		solicitacao.setRequester(requester);
		solicitacao.setStatus(PatientAccessRequestStatus.PENDING);
		solicitacao.setRequestedAt(agora());
		PatientAccessRequest salva = requestRepository.save(solicitacao);
		notificacaoService.criarParaSolicitacaoAcesso(salva, destinatarios);
		registrarEvento(salva, "SOLICITACAO_ACESSO_CRIADA", "VETERINARIO",
				null, requester.getVeterinario().getId(), "Solicitação de acesso ao paciente criada.");
		return respostaParaVeterinario(salva);
	}

	@Transactional(readOnly = true)
	public Page<PatientAccessRequestResponse> listar(
			PatientAccessRequestStatus status,
			Pageable pageable,
			UsuarioPrincipal principal
	) {
		if (principal == null || principal.getTipoUsuario() == null) {
			throw new AccessDeniedException("Usuário autenticado inválido.");
		}
		return switch (principal.getTipoUsuario()) {
			case VETERINARIO -> {
				Usuario requester = exigirSolicitanteVeterinario(principal);
				yield requestRepository.findForRequester(requester.getId(), status, pageable)
						.map(this::respostaParaVeterinario);
			}
			case RESPONSAVEL -> {
				Usuario responsavel = exigirUsuarioResponsavel(principal);
				yield requestRepository.findForCurrentResponsible(
						responsavel.getResponsavel().getId(), hoje(), status, pageable)
						.map(this::respostaParaResponsavel);
			}
			default -> throw new AccessDeniedException("Perfil não autorizado a consultar solicitações de acesso.");
		};
	}

	@Transactional(readOnly = true)
	public PatientAccessRequestResponse buscar(Long id, UsuarioPrincipal principal) {
		PatientAccessRequest solicitacao = buscarDetalhada(id);
		if (TipoUsuario.VETERINARIO.equals(tipo(principal))) {
			Usuario requester = exigirSolicitanteVeterinario(principal);
			if (!Objects.equals(requester.getId(), solicitacao.getRequester().getId())) {
				throw new AccessDeniedException("Veterinário não autorizado a consultar esta solicitação.");
			}
			return respostaParaVeterinario(solicitacao);
		}
		if (TipoUsuario.RESPONSAVEL.equals(tipo(principal))) {
			Usuario responsavel = exigirUsuarioResponsavel(principal);
			exigirVinculoResponsavelAtual(responsavel, solicitacao.getAnimal());
			return respostaParaResponsavel(solicitacao);
		}
		throw new AccessDeniedException("Perfil não autorizado a consultar esta solicitação de acesso.");
	}

	@Transactional
	public PatientAccessRequestResponse aprovar(Long id, UsuarioPrincipal principal) {
		PatientAccessRequest solicitacao = buscarParaResolver(id);
		Usuario aprovador = exigirUsuarioResponsavel(principal);
		exigirVinculoResponsavelAtual(aprovador, solicitacao.getAnimal());

		if (PatientAccessRequestStatus.APPROVED.equals(solicitacao.getStatus())) {
			return respostaParaResponsavel(solicitacao);
		}
		if (PatientAccessRequestStatus.REJECTED.equals(solicitacao.getStatus())) {
			throw new BusinessException("Solicitação rejeitada não pode ser aprovada.", HttpStatus.CONFLICT);
		}
		exigirSolicitanteAindaAtivo(solicitacao.getRequester());
		Animal animal = animalRepository.buscarParaAtualizarVinculos(solicitacao.getAnimal().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Paciente da solicitação não encontrado."));
		PatientAccess access = patientAccessService.grantOrGetExisting(
				animal, solicitacao.getRequester(), PatientAccessRole.VETERINARIO, aprovador);
		solicitacao.setStatus(PatientAccessRequestStatus.APPROVED);
		solicitacao.setResolvedBy(aprovador);
		solicitacao.setResolvedAt(agora());
		solicitacao.setResultingAccess(access);
		PatientAccessRequest salva = requestRepository.save(solicitacao);
		registrarEvento(salva, "SOLICITACAO_ACESSO_APROVADA", "RESPONSAVEL",
				aprovador.getResponsavel().getId(), solicitacao.getRequester().getVeterinario().getId(),
				"Solicitação de acesso ao paciente aprovada.");
		return respostaParaResponsavel(salva);
	}

	@Transactional
	public PatientAccessRequestResponse rejeitar(Long id, UsuarioPrincipal principal) {
		PatientAccessRequest solicitacao = buscarParaResolver(id);
		Usuario resolvedor = exigirUsuarioResponsavel(principal);
		exigirVinculoResponsavelAtual(resolvedor, solicitacao.getAnimal());

		if (PatientAccessRequestStatus.REJECTED.equals(solicitacao.getStatus())) {
			return respostaParaResponsavel(solicitacao);
		}
		if (PatientAccessRequestStatus.APPROVED.equals(solicitacao.getStatus())) {
			throw new BusinessException("Solicitação aprovada não pode ser rejeitada.", HttpStatus.CONFLICT);
		}
		solicitacao.setStatus(PatientAccessRequestStatus.REJECTED);
		solicitacao.setResolvedBy(resolvedor);
		solicitacao.setResolvedAt(agora());
		PatientAccessRequest salva = requestRepository.save(solicitacao);
		registrarEvento(salva, "SOLICITACAO_ACESSO_REJEITADA", "RESPONSAVEL",
				resolvedor.getResponsavel().getId(), solicitacao.getRequester().getVeterinario().getId(),
				"Solicitação de acesso ao paciente rejeitada.");
		return respostaParaResponsavel(salva);
	}

	private Usuario exigirSolicitanteVeterinario(UsuarioPrincipal principal) {
		if (principal == null || principal.getTipoUsuario() != TipoUsuario.VETERINARIO
				|| principal.getUsuarioId() == null || principal.getVeterinarioId() == null) {
			throw new AccessDeniedException("Solicitação de acesso permitida apenas ao veterinário autenticado.");
		}
		Usuario usuario = usuarioRepository.findUsuarioVeterinarioAtivo(principal.getVeterinarioId())
				.orElseThrow(() -> new AccessDeniedException("Perfil veterinário autenticado sem vínculo válido."));
		if (!Objects.equals(usuario.getId(), principal.getUsuarioId())
				|| usuario.getVeterinario() == null
				|| !Objects.equals(usuario.getVeterinario().getId(), principal.getVeterinarioId())
				|| !"S".equals(usuario.getVeterinario().getAtivo())) {
			throw new AccessDeniedException("Perfil veterinário autenticado sem vínculo válido.");
		}
		return usuario;
	}

	private Usuario exigirUsuarioResponsavel(UsuarioPrincipal principal) {
		if (principal == null || principal.getTipoUsuario() != TipoUsuario.RESPONSAVEL
				|| principal.getUsuarioId() == null || principal.getResponsavelId() == null) {
			throw new AccessDeniedException("Resolução permitida apenas ao responsável atual do paciente.");
		}
		Usuario usuario = usuarioRepository.findById(principal.getUsuarioId())
				.orElseThrow(() -> new AccessDeniedException("Usuário autenticado inválido."));
		if (usuario.getTipo() != TipoUsuario.RESPONSAVEL || !"S".equals(usuario.getAtivo())
				|| usuario.getResponsavel() == null || !"S".equals(usuario.getResponsavel().getAtivo())
				|| !Objects.equals(usuario.getResponsavel().getId(), principal.getResponsavelId())) {
			throw new AccessDeniedException("Responsável autenticado sem vínculo válido.");
		}
		return usuario;
	}

	private void exigirVinculoResponsavelAtual(Usuario usuario, Animal animal) {
		if (!relationshipRepository.existsVinculoAtivoVigente(
				animal.getId(), usuario.getResponsavel().getId(), hoje())) {
			throw new AccessDeniedException("Responsável não possui vínculo atual com este paciente.");
		}
	}

	private void exigirSolicitanteAindaAtivo(Usuario requester) {
		if (!TipoUsuario.VETERINARIO.equals(requester.getTipo()) || !"S".equals(requester.getAtivo())
				|| requester.getVeterinario() == null || !"S".equals(requester.getVeterinario().getAtivo())) {
			throw new BusinessException(
					"O veterinário solicitante não está mais ativo; a solicitação permanece pendente.",
					HttpStatus.CONFLICT);
		}
	}

	private PatientAccessRequest buscarDetalhada(Long id) {
		return requestRepository.findDetailedById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Solicitação de acesso não encontrada."));
	}

	private PatientAccessRequest buscarParaResolver(Long id) {
		return requestRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Solicitação de acesso não encontrada."));
	}

	private PatientAccessRequestResponse respostaParaVeterinario(PatientAccessRequest solicitacao) {
		boolean acessoVigente = PatientAccessRequestStatus.APPROVED.equals(solicitacao.getStatus())
				&& solicitacao.getResultingAccess() != null
				&& solicitacao.getResultingAccess().isEffectiveAt(agora());
		return resposta(solicitacao, acessoVigente, false);
	}

	private PatientAccessRequestResponse respostaParaResponsavel(PatientAccessRequest solicitacao) {
		return resposta(solicitacao, true, true);
	}

	private PatientAccessRequestResponse resposta(
			PatientAccessRequest solicitacao,
			boolean mostrarAnimal,
			boolean mostrarResolvedor
	) {
		var veterinario = solicitacao.getRequester().getVeterinario();
		return new PatientAccessRequestResponse(
				solicitacao.getId(),
				solicitacao.getAnimal().getArkivePatientId(),
				mostrarAnimal ? solicitacao.getAnimal().getNome() : null,
				solicitacao.getRequester().getId(),
				veterinario.getId(),
				veterinario.getNome(),
				veterinario.getCrmv(),
				solicitacao.getStatus(),
				solicitacao.getRequestedAt(),
				solicitacao.getResolvedAt(),
				mostrarResolvedor && solicitacao.getResolvedBy() != null
						? solicitacao.getResolvedBy().getId() : null,
				solicitacao.getResultingAccess() == null ? null : solicitacao.getResultingAccess().getId()
		);
	}

	private void registrarEvento(
			PatientAccessRequest solicitacao,
			String tipoEvento,
			String ator,
			Long responsavelId,
			Long veterinarioId,
			String contexto
	) {
		Long clinicaId = solicitacao.getAnimal().getClinica() == null
				? null : solicitacao.getAnimal().getClinica().getId();
		eventoJornadaService.registrarEvento(
				tipoEvento, ator, responsavelId, veterinarioId, solicitacao.getAnimal().getId(), clinicaId,
				contexto,
				eventoJornadaService.criarPayload("PatientAccessRequest", solicitacao.getId(), tipoEvento)
		);
	}

	private String normalizarArkivePatientId(String value) {
		return value.trim().toUpperCase(Locale.ROOT);
	}

	private TipoUsuario tipo(UsuarioPrincipal principal) {
		return principal == null ? null : principal.getTipoUsuario();
	}

	private LocalDateTime agora() {
		return LocalDateTime.now(clock);
	}

	private LocalDate hoje() {
		return LocalDate.now(clock);
	}
}
