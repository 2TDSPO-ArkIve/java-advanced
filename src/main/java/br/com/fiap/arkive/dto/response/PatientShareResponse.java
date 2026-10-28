package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.PatientAccessRole;
import br.com.fiap.arkive.entity.PatientShareToken;

import java.time.LocalDateTime;

public record PatientShareResponse(
		Long invitationId,
		String arkivePatientId,
		PatientAccessRole invitationType,
		Long createdByUserId,
		String createdByName,
		LocalDateTime createdAt,
		LocalDateTime expiresAt,
		LocalDateTime usedAt,
		Long usedByUserId,
		LocalDateTime revokedAt,
		String status
) {
	public static PatientShareResponse fromEntity(PatientShareToken token, LocalDateTime now) {
		String status = token.getRevokedAt() != null ? "REVOKED"
				: token.getUsedAt() != null ? "USED"
				: !token.getExpiresAt().isAfter(now) ? "EXPIRED" : "PENDING";
		return new PatientShareResponse(
				token.getId(), token.getAnimal().getArkivePatientId(), token.getInvitationType(),
				token.getCreatedBy().getId(), token.getCreatedBy().getNome(), token.getCreatedAt(), token.getExpiresAt(),
				token.getUsedAt(), token.getUsedBy() == null ? null : token.getUsedBy().getId(), token.getRevokedAt(), status
		);
	}
}
