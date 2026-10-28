package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Size;

public record ObservacaoResponsavelRequest(
		@Size(max = 2000, message = "Observacao deve ter no maximo 2000 caracteres.")
		String observacao
) {
}
