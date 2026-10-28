package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.CreatePatientShareRequest;
import br.com.fiap.arkive.entity.*;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.*;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PatientShareServiceTest {

	private PatientShareTokenRepository tokens;
	private AnimalRepository animals;
	private UsuarioRepository users;
	private AnimalResponsavelRepository relationships;
	private PatientAccessService accesses;
	private ClinicalAccessService clinicalAccess;
	private ShareTokenCodec codec;
	private PatientShareService service;
	private Animal animal;

	@BeforeEach
	void setUp() {
		tokens = mock(PatientShareTokenRepository.class);
		animals = mock(AnimalRepository.class);
		users = mock(UsuarioRepository.class);
		relationships = mock(AnimalResponsavelRepository.class);
		accesses = mock(PatientAccessService.class);
		clinicalAccess = mock(ClinicalAccessService.class);
		codec = new ShareTokenCodec();
		service = new PatientShareService(tokens, animals, users, relationships, accesses, clinicalAccess, codec, Duration.ofHours(24));
		animal = animal();
		when(animals.findById(50L)).thenReturn(Optional.of(animal));
		when(animals.buscarParaAtualizarVinculos(50L)).thenReturn(Optional.of(animal));
		when(tokens.save(any())).thenAnswer(invocation -> {
			PatientShareToken token = invocation.getArgument(0);
			if (token.getId() == null) token.setId(90L);
			return token;
		});
	}

	@Test
	void allFourCreatorAndTargetDirectionsCanCreateWithoutStoringRawToken() {
		for (TipoUsuario creatorType : new TipoUsuario[]{TipoUsuario.RESPONSAVEL, TipoUsuario.VETERINARIO}) {
			Usuario creator = user(1L, creatorType);
			UsuarioPrincipal principal = principal(creator);
			when(users.findById(1L)).thenReturn(Optional.of(creator));
			for (PatientAccessRole target : PatientAccessRole.values()) {
				var response = service.create(50L, new CreatePatientShareRequest(target), principal);
				assertNotNull(response.rawToken());
				assertEquals(43, response.rawToken().length());
				assertEquals(target, response.invitationType());
			}
		}
		verify(tokens, times(4)).save(argThat(token -> token.getTokenHash().length() == 64
				&& !token.getTokenHash().contains("ARK-PET-") && token.getExpiresAt().isAfter(token.getCreatedAt())));
	}

	@Test
	void unauthorizedCreatorCannotShareAnotherPatient() {
		Usuario creator = user(1L, TipoUsuario.RESPONSAVEL);
		doThrow(new AccessDeniedException("forbidden")).when(clinicalAccess).exigirCriacaoCompartilhamentoAnimal(any(), eq(animal));
		assertThrows(AccessDeniedException.class,
				() -> service.create(50L, new CreatePatientShareRequest(PatientAccessRole.VETERINARIO), principal(creator)));
		verifyNoInteractions(users, tokens);
	}

	@Test
	void veterinarianRedeemsVetInvitationAtomicallyWithoutResponsibleRelationship() {
		Usuario creator = user(1L, TipoUsuario.RESPONSAVEL);
		Usuario recipient = user(2L, TipoUsuario.VETERINARIO);
		PatientShareToken invitation = invitation(PatientAccessRole.VETERINARIO, creator);
		PatientAccess access = access(recipient, creator, PatientAccessRole.VETERINARIO);
		stubRedeem("secret", invitation, recipient, access);

		var response = service.redeem("secret", principal(recipient));

		assertEquals(PatientAccessRole.VETERINARIO, response.accessRole());
		assertNotNull(invitation.getUsedAt());
		assertSame(recipient, invitation.getUsedBy());
		assertSame(access, invitation.getResultingAccess());
		verify(relationships, never()).save(any());
	}

	@Test
	void veterinarianCanShareWithAnotherVeterinarian() {
		Usuario creator = user(1L, TipoUsuario.VETERINARIO);
		Usuario recipient = user(2L, TipoUsuario.VETERINARIO);
		PatientShareToken invitation = invitation(PatientAccessRole.VETERINARIO, creator);
		PatientAccess access = access(recipient, creator, PatientAccessRole.VETERINARIO);
		stubRedeem("vet-to-vet", invitation, recipient, access);

		assertEquals(PatientAccessRole.VETERINARIO,
				service.redeem("vet-to-vet", principal(recipient)).accessRole());
		assertNotNull(invitation.getUsedAt());
		verify(relationships, never()).save(any());
	}

	@Test
	void firstResponsibleRedemptionCreatesPrincipalLegalRelationshipAndGrant() {
		Usuario creator = user(1L, TipoUsuario.VETERINARIO);
		Usuario recipient = user(2L, TipoUsuario.RESPONSAVEL);
		PatientShareToken invitation = invitation(PatientAccessRole.RESPONSAVEL, creator);
		PatientAccess access = access(recipient, creator, PatientAccessRole.RESPONSAVEL);
		stubRedeem("secret", invitation, recipient, access);
		when(relationships.existsVinculoAtivoVigente(eq(50L), eq(102L), any(LocalDate.class))).thenReturn(false);
		when(relationships.existsQualquerVinculoAtivoVigente(eq(50L), any(LocalDate.class))).thenReturn(false);
		when(relationships.findById(any())).thenReturn(Optional.empty());

		service.redeem("secret", principal(recipient));

		verify(relationships).save(argThat(link -> "TUTOR_LEGAL".equals(link.getTipoVinculo())
				&& "S".equals(link.getAtivo()) && "S".equals(link.getPrincipal())));
	}

	@Test
	void laterResponsibleRedemptionCreatesNonPrincipalCaregiver() {
		Usuario creator = user(1L, TipoUsuario.VETERINARIO);
		Usuario recipient = user(2L, TipoUsuario.RESPONSAVEL);
		PatientShareToken invitation = invitation(PatientAccessRole.RESPONSAVEL, creator);
		stubRedeem("later", invitation, recipient, access(recipient, creator, PatientAccessRole.RESPONSAVEL));
		when(relationships.existsVinculoAtivoVigente(eq(50L), eq(102L), any(LocalDate.class))).thenReturn(false);
		when(relationships.existsQualquerVinculoAtivoVigente(eq(50L), any(LocalDate.class))).thenReturn(true);
		when(relationships.findById(any())).thenReturn(Optional.empty());

		service.redeem("later", principal(recipient));

		verify(relationships).save(argThat(link -> "CUIDADOR".equals(link.getTipoVinculo())
				&& "S".equals(link.getAtivo()) && "N".equals(link.getPrincipal())));
		var order = inOrder(tokens, animals, relationships);
		order.verify(tokens).findByTokenHashForUpdate(codec.hash("later"));
		order.verify(animals).buscarParaAtualizarVinculos(50L);
		order.verify(relationships).existsVinculoAtivoVigente(eq(50L), eq(102L), any(LocalDate.class));
		order.verify(relationships).existsQualquerVinculoAtivoVigente(eq(50L), any(LocalDate.class));
	}

	@Test
	void existingResponsibleRelationshipIsIdempotent() {
		Usuario creator = user(1L, TipoUsuario.VETERINARIO);
		Usuario recipient = user(2L, TipoUsuario.RESPONSAVEL);
		stubRedeem("secret", invitation(PatientAccessRole.RESPONSAVEL, creator), recipient,
				access(recipient, creator, PatientAccessRole.RESPONSAVEL));
		when(relationships.existsVinculoAtivoVigente(eq(50L), eq(102L), any(LocalDate.class))).thenReturn(true);
		service.redeem("secret", principal(recipient));
		verify(relationships, never()).save(any());
	}

	@Test
	void responsibleCanShareWithAnotherResponsible() {
		Usuario creator = user(1L, TipoUsuario.RESPONSAVEL);
		Usuario recipient = user(2L, TipoUsuario.RESPONSAVEL);
		PatientShareToken invitation = invitation(PatientAccessRole.RESPONSAVEL, creator);
		stubRedeem("responsible-to-responsible", invitation, recipient,
				access(recipient, creator, PatientAccessRole.RESPONSAVEL));
		when(relationships.existsVinculoAtivoVigente(eq(50L), eq(102L), any(LocalDate.class))).thenReturn(true);

		assertEquals(PatientAccessRole.RESPONSAVEL,
				service.redeem("responsible-to-responsible", principal(recipient)).accessRole());
		assertNotNull(invitation.getUsedAt());
	}

	@Test
	void invitationSurvivesUntilAnAccountAuthenticatesAndExplicitlyRedeems() {
		Usuario creator = user(1L, TipoUsuario.VETERINARIO);
		Usuario recipient = user(2L, TipoUsuario.RESPONSAVEL);
		PatientShareToken invitation = invitation(PatientAccessRole.RESPONSAVEL, creator);
		when(tokens.findByTokenHashForUpdate(codec.hash("deferred"))).thenReturn(Optional.of(invitation));

		assertThrows(AccessDeniedException.class, () -> service.redeem("deferred", null));
		assertNull(invitation.getUsedAt());
		verifyNoInteractions(accesses);

		when(users.findById(2L)).thenReturn(Optional.of(recipient));
		when(accesses.grantOrGetExisting(animal, recipient, PatientAccessRole.RESPONSAVEL, creator))
				.thenReturn(access(recipient, creator, PatientAccessRole.RESPONSAVEL));
		when(relationships.existsVinculoAtivoVigente(eq(50L), eq(102L), any(LocalDate.class))).thenReturn(true);

		service.redeem("deferred", principal(recipient));
		assertNotNull(invitation.getUsedAt());
		assertSame(recipient, invitation.getUsedBy());
	}

	@Test
	void previewNeverConsumesInvitation() {
		Usuario creator = user(1L, TipoUsuario.VETERINARIO);
		PatientShareToken invitation = invitation(PatientAccessRole.RESPONSAVEL, creator);
		when(tokens.findByTokenHash(codec.hash("preview"))).thenReturn(Optional.of(invitation));

		var response = service.preview("preview");

		assertTrue(response.valid());
		assertEquals("PENDING", response.status());
		assertNull(invitation.getUsedAt());
		verify(tokens, never()).save(invitation);
	}

	@Test
	void wrongRoleDoesNotConsumeToken() {
		Usuario creator = user(1L, TipoUsuario.VETERINARIO);
		Usuario responsible = user(2L, TipoUsuario.RESPONSAVEL);
		PatientShareToken invitation = invitation(PatientAccessRole.VETERINARIO, creator);
		when(tokens.findByTokenHashForUpdate(codec.hash("secret"))).thenReturn(Optional.of(invitation));
		when(users.findById(2L)).thenReturn(Optional.of(responsible));
		assertThrows(BusinessException.class, () -> service.redeem("secret", principal(responsible)));
		assertNull(invitation.getUsedAt());
		verify(accesses, never()).grantOrGetExisting(any(), any(), any(), any());
	}

	@Test
	void expiredRevokedUsedAndUnknownTokensAreRejected() {
		Usuario creator = user(1L, TipoUsuario.VETERINARIO);
		Usuario recipient = user(2L, TipoUsuario.VETERINARIO);
		for (String raw : new String[]{"expired", "revoked", "used"}) {
			PatientShareToken token = invitation(PatientAccessRole.VETERINARIO, creator);
			if (raw.equals("expired")) token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
			if (raw.equals("revoked")) token.setRevokedAt(LocalDateTime.now());
			if (raw.equals("used")) token.setUsedAt(LocalDateTime.now());
			when(tokens.findByTokenHashForUpdate(codec.hash(raw))).thenReturn(Optional.of(token));
			assertThrows(BusinessException.class, () -> service.redeem(raw, principal(recipient)));
		}
		when(tokens.findByTokenHashForUpdate(codec.hash("unknown"))).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> service.redeem("unknown", principal(recipient)));
	}

	@Test
	void creatorCanRevokeUnusedButUsedInvitationRequiresSeparateAccessRevocation() {
		Usuario creator = user(1L, TipoUsuario.RESPONSAVEL);
		PatientShareToken invitation = invitation(PatientAccessRole.VETERINARIO, creator);
		invitation.setId(90L);
		when(tokens.findByIdForUpdate(90L)).thenReturn(Optional.of(invitation));
		when(users.findById(1L)).thenReturn(Optional.of(creator));
		assertEquals("REVOKED", service.revoke(90L, principal(creator)).status());
		invitation.setRevokedAt(null);
		invitation.setRevokedBy(null);
		invitation.setUsedAt(LocalDateTime.now());
		assertThrows(BusinessException.class, () -> service.revoke(90L, principal(creator)));
	}

	private void stubRedeem(String raw, PatientShareToken invitation, Usuario recipient, PatientAccess access) {
		when(tokens.findByTokenHashForUpdate(codec.hash(raw))).thenReturn(Optional.of(invitation));
		when(users.findById(recipient.getId())).thenReturn(Optional.of(recipient));
		when(accesses.grantOrGetExisting(animal, recipient, invitation.getInvitationType(), invitation.getCreatedBy())).thenReturn(access);
	}

	private PatientShareToken invitation(PatientAccessRole role, Usuario creator) {
		PatientShareToken token = new PatientShareToken();
		token.setId(90L); token.setAnimal(animal); token.setTokenHash(codec.hash("secret"));
		token.setCreatedBy(creator); token.setInvitationType(role); token.setCreatedAt(LocalDateTime.now());
		token.setExpiresAt(LocalDateTime.now().plusHours(1));
		return token;
	}

	private PatientAccess access(Usuario recipient, Usuario creator, PatientAccessRole role) {
		PatientAccess access = new PatientAccess();
		access.setId(70L); access.setAnimal(animal); access.setUsuario(recipient); access.setGrantedBy(creator);
		access.setAccessRole(role); access.setGrantedAt(LocalDateTime.now());
		return access;
	}

	private Animal animal() {
		Especie species = new Especie(); species.setId(5L); species.setNome("Canino");
		Animal value = new Animal(); value.setId(50L); value.setNome("Nina"); value.setEspecie(species);
		value.setAtivo("S"); value.assignArkivePatientId("ARK-PET-22222-33333-44444-55555-666666");
		return value;
	}

	private Usuario user(Long id, TipoUsuario type) {
		Usuario user = new Usuario(); user.setId(id); user.setNome("User " + id); user.setTipo(type); user.setAtivo("S");
		if (type == TipoUsuario.VETERINARIO) {
			Veterinario vet = new Veterinario(); vet.setId(100L + id); vet.setAtivo("S"); user.setVeterinario(vet);
		}
		if (type == TipoUsuario.RESPONSAVEL) {
			Responsavel responsible = new Responsavel(); responsible.setId(100L + id); responsible.setAtivo("S"); user.setResponsavel(responsible);
		}
		return user;
	}

	private UsuarioPrincipal principal(Usuario user) {
		return new UsuarioPrincipal(user.getId(), user.getNome(), "u@x", "hash", user.getTipo(), "S", false,
				user.getResponsavel() == null ? null : user.getResponsavel().getId(),
				user.getVeterinario() == null ? null : user.getVeterinario().getId(), null);
	}
}
