package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record FeedbackNpsRequest(
		Long responsavelId,

		Long animalId,

		Long clinicaId,

		Long consultaId,

		@NotNull
		@Min(0)
		@Max(10)
		Integer nota,

		@Size(max = 1000)
		String comentario,

		LocalDateTime dataResposta
) {
}
