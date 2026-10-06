package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.PatientAccess;
import br.com.fiap.arkive.entity.PatientAccessRole;

import java.time.LocalDateTime;

public record PatientAccessResponse(
		Long id,
		Long animalId,
		String arkivePatientId,
		Long userId,
		String userName,
		PatientAccessRole accessRole,
		Long grantedByUserId,
		LocalDateTime grantedAt,
		LocalDateTime expiresAt,
		Long revokedByUserId,
		LocalDateTime revokedAt,
		boolean effective
) {
	public static PatientAccessResponse fromEntity(PatientAccess access, LocalDateTime now) {
		return new PatientAccessResponse(
				access.getId(), access.getAnimal().getId(), access.getAnimal().getArkivePatientId(),
				access.getUsuario().getId(), access.getUsuario().getNome(), access.getAccessRole(),
				access.getGrantedBy().getId(), access.getGrantedAt(), access.getExpiresAt(),
				access.getRevokedBy() == null ? null : access.getRevokedBy().getId(), access.getRevokedAt(),
				access.isEffectiveAt(now)
		);
	}
}
