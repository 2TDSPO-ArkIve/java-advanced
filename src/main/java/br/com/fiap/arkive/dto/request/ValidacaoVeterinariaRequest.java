package br.com.fiap.arkive.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Avaliacao do veterinario sobre a utilidade de um insight clinico gerado por IA.")
public record ValidacaoVeterinariaRequest(
		@NotNull
		@Schema(description = "Indica se o insight de IA foi util.", example = "true")
		Boolean util
) {
}
