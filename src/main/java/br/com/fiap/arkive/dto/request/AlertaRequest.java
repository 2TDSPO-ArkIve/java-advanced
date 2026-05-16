package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AlertaRequest(
		@NotBlank
		@Size(max = 50)
		String tipo,

		@NotBlank
		String mensagem,

		LocalDateTime dataEnvio,

		LocalDateTime dataLeitura,

		@Size(max = 20)
		String status,

		@NotBlank
		@Size(max = 20)
		String canal,

		@NotNull
		Long animalId,

		Long responsavelId,

		Long clinicaId,

		Long eventoPreventivoId
) {
}
