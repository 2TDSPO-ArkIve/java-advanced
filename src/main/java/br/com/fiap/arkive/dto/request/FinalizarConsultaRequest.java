package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinalizarConsultaRequest(
		@NotBlank
		String diagnostico,

		@Size(max = 20)
		String severidade,

		Long doencaId,

		String conclusao
) {
}
