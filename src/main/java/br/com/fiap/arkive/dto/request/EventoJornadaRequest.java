package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventoJornadaRequest(
		@NotBlank
		@Size(max = 50)
		String tipoEvento,

		@NotBlank
		@Size(max = 30)
		String origem,

		@Size(max = 30)
		String ator,

		Long responsavelId,

		Long veterinarioId,

		Long animalId,

		Long clinicaId,

		@Size(max = 30)
		String canal,

		String contexto,

		String payloadJson
) {
}
