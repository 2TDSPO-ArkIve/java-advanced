package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.PatientAccessResponse;
import br.com.fiap.arkive.entity.*;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.PatientAccessRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("!local-nodb")
public class PatientAccessService {

	private final PatientAccessRepository repository;
	private final AnimalRepository animalRepository;
	private final UsuarioRepository usuarioRepository;
	private final ClinicalAccessService clinicalAccessService;

	public PatientAccessService(PatientAccessRepository repository, AnimalRepository animalRepository,
			UsuarioRepository usuarioRepository, ClinicalAccessService clinicalAccessService) {
		this.repository = repository;
		this.animalRepository = animalRepository;
		this.usuarioRepository = usuarioRepository;
		this.clinicalAccessService = clinicalAccessService;
	}

	@Transactional(readOnly = true)
	public List<PatientAccessResponse> list(Long animalId, UsuarioPrincipal principal) {
		Animal animal = findAnimal(animalId);
		clinicalAccessService.exigirCompartilhamentoAnimal(principal, animal);
		LocalDateTime now = LocalDateTime.now();
		return repository.findByAnimalIdOrderByGrantedAtDesc(animalId).stream()
				.map(access -> PatientAccessResponse.fromEntity(access, now)).toList();
	}

	@Transactional
	public PatientAccessResponse revoke(Long animalId, Long accessId, UsuarioPrincipal principal) {
		Animal animal = animalRepository.buscarParaAtualizarVinculos(animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
		PatientAccess access = repository.findByIdAndAnimalId(accessId, animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Acesso explicito nao encontrado."));
		if (access.getRevokedAt() != null) {
			return PatientAccessResponse.fromEntity(access, LocalDateTime.now());
		}
		if (!canRevoke(principal, access, animal)) {
			throw new AccessDeniedException("Usuario nao autorizado a revogar este acesso.");
		}
		Usuario actor = findActiveUser(principal);
		access.setRevokedAt(LocalDateTime.now());
		access.setRevokedBy(actor);
		return PatientAccessResponse.fromEntity(repository.save(access), LocalDateTime.now());
	}

	PatientAccess grantOrGetExisting(Animal animal, Usuario recipient, PatientAccessRole role, Usuario grantor) {
		List<PatientAccess> existing = repository.findEffective(animal.getId(), recipient.getId(), role, LocalDateTime.now());
		if (!existing.isEmpty()) {
			return existing.get(0);
		}
		PatientAccess access = new PatientAccess();
		access.setAnimal(animal);
		access.setUsuario(recipient);
		access.setAccessRole(role);
		access.setGrantedBy(grantor);
		access.setGrantedAt(LocalDateTime.now());
		return repository.save(access);
	}

	private boolean canRevoke(UsuarioPrincipal principal, PatientAccess access, Animal animal) {
		if (principal == null) return false;
		if (TipoUsuario.SYSADMIN.equals(principal.getTipoUsuario())) return true;
		if (principal.getUsuarioId() != null && principal.getUsuarioId().equals(access.getGrantedBy().getId())) return true;
		return TipoUsuario.RESPONSAVEL.equals(principal.getTipoUsuario()) && principal.getResponsavelId() != null
				&& clinicalAccessService.temVinculoResponsavelAtivo(principal.getResponsavelId(), animal.getId());
	}

	private Usuario findActiveUser(UsuarioPrincipal principal) {
		if (principal == null || principal.getUsuarioId() == null) throw new AccessDeniedException("Usuario autenticado invalido.");
		Usuario user = usuarioRepository.findById(principal.getUsuarioId())
				.orElseThrow(() -> new AccessDeniedException("Usuario autenticado invalido."));
		if (!"S".equals(user.getAtivo())) throw new AccessDeniedException("Usuario inativo.");
		return user;
	}

	private Animal findAnimal(Long animalId) {
		return animalRepository.findById(animalId).orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}
}
