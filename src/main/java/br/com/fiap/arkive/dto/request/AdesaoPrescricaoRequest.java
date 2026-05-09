package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdesaoPrescricaoRequest(
		@NotNull
		Long prescricaoId,

		Long responsavelId,

		@NotNull
		Long animalId,

		LocalDateTime dataRegistro,

		@NotBlank
		@Size(max = 1)
		String tomou,

		@Size(max = 500)
		String observacao
) {
}
