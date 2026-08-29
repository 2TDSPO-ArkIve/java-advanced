package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AtualizarNarrativaConsultaRequest(
		@NotBlank
		String narrativa
) {
}
