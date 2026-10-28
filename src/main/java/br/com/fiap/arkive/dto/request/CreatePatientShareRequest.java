package br.com.fiap.arkive.dto.request;

import br.com.fiap.arkive.entity.PatientAccessRole;
import jakarta.validation.constraints.NotNull;

public record CreatePatientShareRequest(@NotNull PatientAccessRole invitationType) {
}
