package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.PatientAccessRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Solicitação autenticada de acesso veterinário ao paciente.")
public record PatientAccessRequestResponse(
		Long id,
		String arkivePatientId,
		@Schema(description = "Nome visível ao responsável; para o solicitante, somente após acesso aprovado e vigente.")
		String animalNome,
		Long requesterUserId,
		Long veterinarioId,
		String veterinarioNome,
		String crmv,
		PatientAccessRequestStatus status,
		LocalDateTime requestedAt,
		LocalDateTime resolvedAt,
		@Schema(description = "Identificador do resolvedor, nunca exposto ao veterinário solicitante.")
		Long resolvedByUserId,
		Long resultingAccessId
) {
}
