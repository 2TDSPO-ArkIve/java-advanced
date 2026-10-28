package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.CreatePatientShareRequest;
import br.com.fiap.arkive.dto.response.*;
import br.com.fiap.arkive.entity.*;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.*;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("!local-nodb")
public class PatientShareService {

	private final PatientShareTokenRepository tokenRepository;
	private final AnimalRepository animalRepository;
	private final UsuarioRepository usuarioRepository;
	private final AnimalResponsavelRepository relationshipRepository;
	private final PatientAccessService patientAccessService;
	private final ClinicalAccessService clinicalAccessService;
	private final ShareTokenCodec tokenCodec;
	private final Duration tokenTtl;

	public PatientShareService(PatientShareTokenRepository tokenRepository, AnimalRepository animalRepository,
			UsuarioRepository usuarioRepository, AnimalResponsavelRepository relationshipRepository,
			PatientAccessService patientAccessService, ClinicalAccessService clinicalAccessService,
			ShareTokenCodec tokenCodec,
			@Value("${arkive.patient-sharing.token-ttl:PT24H}") Duration tokenTtl) {
		this.tokenRepository = tokenRepository;
		this.animalRepository = animalRepository;
		this.usuarioRepository = usuarioRepository;
		this.relationshipRepository = relationshipRepository;
		this.patientAccessService = patientAccessService;
		this.clinicalAccessService = clinicalAccessService;
		this.tokenCodec = tokenCodec;
		this.tokenTtl = tokenTtl;
	}

	@Transactional
	public PatientShareCreatedResponse create(Long animalId, CreatePatientShareRequest request, UsuarioPrincipal principal) {
		Animal animal = animalRepository.findById(animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
		clinicalAccessService.exigirCriacaoCompartilhamentoAnimal(principal, animal);
		Usuario creator = findActiveUser(principal);
		String rawToken = generateUniqueToken();
		PatientShareToken invitation = new PatientShareToken();
		invitation.setAnimal(animal);
		invitation.setTokenHash(tokenCodec.hash(rawToken));
		invitation.setCreatedBy(creator);
		invitation.setInvitationType(request.invitationType());
		invitation.setCreatedAt(LocalDateTime.now());
		invitation.setExpiresAt(invitation.getCreatedAt().plus(tokenTtl));
		PatientShareToken saved = tokenRepository.save(invitation);
		return new PatientShareCreatedResponse(saved.getId(), rawToken, saved.getInvitationType(), saved.getExpiresAt());
	}

	@Transactional(readOnly = true)
	public PatientSharePreviewResponse preview(String rawToken) {
		PatientShareToken token = tokenRepository.findByTokenHash(requireHash(rawToken))
				.orElseThrow(() -> new ResourceNotFoundException("Convite de compartilhamento invalido."));
		LocalDateTime now = LocalDateTime.now();
		String status = status(token, now);
		boolean valid = "PENDING".equals(status);
		return new PatientSharePreviewResponse(valid, status, token.getAnimal().getNome(),
				token.getAnimal().getEspecie().getNome(), token.getInvitationType(),
				token.getCreatedBy().getNome(), token.getExpiresAt());
	}

	@Transactional
	public PatientAccessResponse redeem(String rawToken, UsuarioPrincipal principal) {
		PatientShareToken invitation = tokenRepository.findByTokenHashForUpdate(requireHash(rawToken))
				.orElseThrow(() -> new ResourceNotFoundException("Convite de compartilhamento invalido."));
		LocalDateTime now = LocalDateTime.now();
		validatePending(invitation, now);
		Usuario recipient = findActiveUser(principal);
		validateRecipientRole(invitation.getInvitationType(), recipient);
		Animal animal = animalRepository.buscarParaAtualizarVinculos(invitation.getAnimal().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));

		PatientAccess access = patientAccessService.grantOrGetExisting(
				animal, recipient, invitation.getInvitationType(), invitation.getCreatedBy());
		if (PatientAccessRole.RESPONSAVEL.equals(invitation.getInvitationType())) {
			ensureResponsibleRelationship(animal, recipient.getResponsavel());
		}
		invitation.setUsedAt(now);
		invitation.setUsedBy(recipient);
		invitation.setResultingAccess(access);
		tokenRepository.save(invitation);
		return PatientAccessResponse.fromEntity(access, now);
	}

	@Transactional
	public PatientShareResponse revoke(Long invitationId, UsuarioPrincipal principal) {
		PatientShareToken invitation = tokenRepository.findByIdForUpdate(invitationId)
				.orElseThrow(() -> new ResourceNotFoundException("Convite de compartilhamento nao encontrado."));
		Usuario actor = findActiveUser(principal);
		if (!TipoUsuario.SYSADMIN.equals(actor.getTipo()) && !actor.getId().equals(invitation.getCreatedBy().getId())) {
			throw new AccessDeniedException("Somente o criador ou SYSADMIN pode revogar este convite.");
		}
		if (invitation.getUsedAt() != null) {
			throw new BusinessException("Convite ja utilizado; revogue o acesso explicitamente.", HttpStatus.CONFLICT);
		}
		if (invitation.getRevokedAt() != null) return PatientShareResponse.fromEntity(invitation, LocalDateTime.now());
		invitation.setRevokedAt(LocalDateTime.now());
		invitation.setRevokedBy(actor);
		return PatientShareResponse.fromEntity(tokenRepository.save(invitation), LocalDateTime.now());
	}

	@Transactional(readOnly = true)
	public List<PatientShareResponse> list(Long animalId, UsuarioPrincipal principal) {
		Animal animal = animalRepository.findById(animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
		clinicalAccessService.exigirCriacaoCompartilhamentoAnimal(principal, animal);
		LocalDateTime now = LocalDateTime.now();
		return tokenRepository.findByAnimalIdOrderByCreatedAtDesc(animalId).stream()
				.map(token -> PatientShareResponse.fromEntity(token, now)).toList();
	}

	private void ensureResponsibleRelationship(Animal animal, Responsavel responsible) {
		LocalDate today = LocalDate.now();
		if (relationshipRepository.existsVinculoAtivoVigente(animal.getId(), responsible.getId(), today)) return;
		boolean firstResponsible = !relationshipRepository.existsQualquerVinculoAtivoVigente(animal.getId(), today);
		AnimalResponsavelId id = new AnimalResponsavelId();
		id.setAnimalId(animal.getId());
		id.setResponsavelId(responsible.getId());
		id.setDataInicio(today);
		AnimalResponsavel relationship = relationshipRepository.findById(id).orElseGet(AnimalResponsavel::new);
		relationship.setId(id);
		relationship.setAnimal(animal);
		relationship.setResponsavel(responsible);
		relationship.setTipoVinculo(firstResponsible ? "TUTOR_LEGAL" : "CUIDADOR");
		relationship.setDataFim(null);
		relationship.setPrincipal(firstResponsible ? "S" : "N");
		relationship.setAtivo("S");
		relationshipRepository.save(relationship);
	}

	private void validateRecipientRole(PatientAccessRole invitationType, Usuario recipient) {
		boolean valid = switch (invitationType) {
			case VETERINARIO -> TipoUsuario.VETERINARIO.equals(recipient.getTipo())
					&& recipient.getVeterinario() != null && "S".equals(recipient.getVeterinario().getAtivo());
			case RESPONSAVEL -> TipoUsuario.RESPONSAVEL.equals(recipient.getTipo())
					&& recipient.getResponsavel() != null && "S".equals(recipient.getResponsavel().getAtivo());
		};
		if (!valid) throw new BusinessException("O perfil autenticado nao corresponde ao tipo do convite.", HttpStatus.FORBIDDEN);
	}

	private void validatePending(PatientShareToken token, LocalDateTime now) {
		if (token.getUsedAt() != null) throw new BusinessException("Convite de compartilhamento ja utilizado.", HttpStatus.CONFLICT);
		if (token.getRevokedAt() != null) throw new BusinessException("Convite de compartilhamento revogado.", HttpStatus.GONE);
		if (!token.getExpiresAt().isAfter(now)) throw new BusinessException("Convite de compartilhamento expirado.", HttpStatus.GONE);
	}

	private String status(PatientShareToken token, LocalDateTime now) {
		if (token.getRevokedAt() != null) return "REVOKED";
		if (token.getUsedAt() != null) return "USED";
		if (!token.getExpiresAt().isAfter(now)) return "EXPIRED";
		return "PENDING";
	}

	private Usuario findActiveUser(UsuarioPrincipal principal) {
		if (principal == null || principal.getUsuarioId() == null) throw new AccessDeniedException("Usuario autenticado invalido.");
		Usuario user = usuarioRepository.findById(principal.getUsuarioId())
				.orElseThrow(() -> new AccessDeniedException("Usuario autenticado invalido."));
		if (!"S".equals(user.getAtivo())) throw new AccessDeniedException("Usuario inativo.");
		return user;
	}

	private String generateUniqueToken() {
		for (int attempt = 0; attempt < 10; attempt++) {
			String raw = tokenCodec.generateRawToken();
			if (!tokenRepository.existsByTokenHash(tokenCodec.hash(raw))) return raw;
		}
		throw new BusinessException("Nao foi possivel gerar o convite de compartilhamento.", HttpStatus.CONFLICT);
	}

	private String requireHash(String rawToken) {
		String hash = tokenCodec.hash(rawToken);
		if (hash == null) throw new BusinessException("Token de compartilhamento deve ser informado.");
		return hash;
	}
}
