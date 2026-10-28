package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePatientAccessRequest(
		@NotBlank(message = "ArkIve ID é obrigatório.")
		@Size(max = 40, message = "ArkIve ID deve ter no máximo 40 caracteres.")
		String arkivePatientId
) {
}
