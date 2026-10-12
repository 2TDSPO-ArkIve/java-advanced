package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.PatientAccessRole;

import java.time.LocalDateTime;

public record PatientSharePreviewResponse(
		boolean valid,
		String status,
		String patientName,
		String speciesName,
		PatientAccessRole invitationType,
		String invitedByName,
		LocalDateTime expiresAt
) {
}
