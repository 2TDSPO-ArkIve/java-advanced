package br.com.fiap.arkive.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DiagnosticoRequest(
		@NotBlank
		String diagnostico,

		@Size(max = 20)
		@Schema(description = "Severidade: AUSENTE, LEVE, MODERADA ou GRAVE. Nulo significa nao informado.",
				allowableValues = {"AUSENTE", "LEVE", "MODERADA", "GRAVE"}, nullable = true)
		String severidade,

		@Size(max = 1)
		String confirmado,

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
