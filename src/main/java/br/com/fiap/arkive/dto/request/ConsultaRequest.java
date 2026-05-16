package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultaRequest(
		@NotNull
		LocalDateTime dataHora,

		@NotBlank
		@Size(max = 20)
		String modalidade,

		@NotBlank
		String motivo,

		@Size(max = 1000)
		String sintomas,

		@Size(max = 2000)
		String observacao,

		@Positive
		BigDecimal peso,

		String transcricao,

		@Size(max = 2)
		String status,

		@NotNull
		Long animalId,

		@NotNull
		Long veterinarioId,

		Long clinicaId
) {
}
