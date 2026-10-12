package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.PatientAccessRole;

import java.time.LocalDateTime;

public record PatientShareCreatedResponse(
		Long invitationId,
		String rawToken,
		PatientAccessRole invitationType,
		LocalDateTime expiresAt
) {
}
