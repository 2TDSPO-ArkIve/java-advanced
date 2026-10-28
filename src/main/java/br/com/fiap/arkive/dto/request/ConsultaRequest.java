package br.com.fiap.arkive.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

		String sintomas,

		String observacao,

		@Positive
		BigDecimal peso,

		String transcricao,

		@Size(max = 2)
		String status,

		@NotNull
		Long animalId,

		Long veterinarioId,

		Long clinicaId,

		@Size(max = 255)
		String endereco,

		@DecimalMin("0.00")
		@Digits(integer = 8, fraction = 2)
		@Schema(description = "Custo assistencial registrado para a consulta. Nulo significa que nenhum valor foi informado.", example = "180.00")
		BigDecimal valorConsulta
) {
	public ConsultaRequest(LocalDateTime dataHora, String modalidade, String motivo, String sintomas,
			String observacao, BigDecimal peso, String transcricao, String status, Long animalId,
			Long veterinarioId, Long clinicaId) {
		this(dataHora, modalidade, motivo, sintomas, observacao, peso, transcricao, status,
				animalId, veterinarioId, clinicaId, null, null);
	}

	public ConsultaRequest(LocalDateTime dataHora, String modalidade, String motivo, String sintomas,
			String observacao, BigDecimal peso, String transcricao, String status, Long animalId,
			Long veterinarioId, Long clinicaId, String endereco) {
		this(dataHora, modalidade, motivo, sintomas, observacao, peso, transcricao, status,
				animalId, veterinarioId, clinicaId, endereco, null);
	}
}
