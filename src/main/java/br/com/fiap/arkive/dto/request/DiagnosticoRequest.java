package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DiagnosticoRequest(
		@NotBlank
		@Size(max = 1000)
		String diagnostico,

		@Size(max = 20)
		String severidade,

		@Size(max = 1)
		String confirmado,

		@Size(max = 1000)
		String insightIa,

		@DecimalMin("0")
		@DecimalMax("100")
		BigDecimal confianca,

		@Size(max = 1)
		String validacaoVet,

		@NotNull
		Long consultaId,

		Long doencaId
) {
}
