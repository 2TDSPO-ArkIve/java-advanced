package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.*;
import br.com.fiap.arkive.repository.*;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PatientAccessServiceTest {

	@Test
	void duplicateEffectiveGrantIsIdempotent() {
		PatientAccessRepository repository = mock(PatientAccessRepository.class);
		PatientAccessService service = new PatientAccessService(repository, mock(AnimalRepository.class),
				mock(UsuarioRepository.class), mock(ClinicalAccessService.class));
		Animal animal = new Animal(); animal.setId(1L);
		Usuario recipient = user(2L, TipoUsuario.VETERINARIO);
		Usuario grantor = user(3L, TipoUsuario.RESPONSAVEL);
		PatientAccess existing = new PatientAccess(); existing.setId(4L);
		when(repository.findEffective(eq(1L), eq(2L), eq(PatientAccessRole.VETERINARIO), any(LocalDateTime.class)))
				.thenReturn(List.of(existing));
		assertSame(existing, service.grantOrGetExisting(animal, recipient, PatientAccessRole.VETERINARIO, grantor));
		verify(repository, never()).save(any());
	}

	@Test
	void revocationSoftDeletesAndRemovesEffectiveness() {
		PatientAccessRepository repository = mock(PatientAccessRepository.class);
		AnimalRepository animals = mock(AnimalRepository.class);
		UsuarioRepository users = mock(UsuarioRepository.class);
		ClinicalAccessService clinical = mock(ClinicalAccessService.class);
		PatientAccessService service = new PatientAccessService(repository, animals, users, clinical);
		Animal animal = new Animal(); animal.setId(1L); animal.assignArkivePatientId("ARK-PET-22222-33333-44444-55555-666666");
		Usuario grantor = user(3L, TipoUsuario.RESPONSAVEL);
		PatientAccess access = new PatientAccess(); access.setId(4L); access.setAnimal(animal); access.setUsuario(user(2L, TipoUsuario.VETERINARIO));
		access.setAccessRole(PatientAccessRole.VETERINARIO); access.setGrantedBy(grantor); access.setGrantedAt(LocalDateTime.now());
		when(animals.buscarParaAtualizarVinculos(1L)).thenReturn(Optional.of(animal));
		when(repository.findByIdAndAnimalId(4L, 1L)).thenReturn(Optional.of(access));
		when(users.findById(3L)).thenReturn(Optional.of(grantor));
		when(repository.save(access)).thenReturn(access);
		UsuarioPrincipal principal = new UsuarioPrincipal(3L, "Grantor", "g", "h", TipoUsuario.RESPONSAVEL, "S", false, 103L, null, null);
		var response = service.revoke(1L, 4L, principal);
		assertNotNull(response.revokedAt());
		assertFalse(response.effective());
		assertSame(grantor, access.getRevokedBy());
	}

	private Usuario user(Long id, TipoUsuario type) {
		Usuario user = new Usuario(); user.setId(id); user.setNome("U" + id); user.setTipo(type); user.setAtivo("S");
		return user;
	}
}
