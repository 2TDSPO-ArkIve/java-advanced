package br.com.fiap.arkive.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Conclusao diagnostica ou clinica registrada pelo veterinario para finalizar uma consulta. O suporte ArkIve nao e obrigatorio.")
public record FinalizarConsultaRequest(
		@NotBlank
		@Schema(description = "Conclusao diagnostica ou clinica confirmada pelo veterinario, inclusive consulta de rotina ou nenhuma doenca identificada.", example = "Consulta de rotina")
		String diagnostico,

		@Size(max = 20)
		@Schema(description = "Severidade opcional da conclusao veterinaria. AUSENTE significa ausencia clinica de severidade; nulo significa nao informado.", example = "AUSENTE", nullable = true,
				allowableValues = {"AUSENTE", "LEVE", "MODERADA", "GRAVE"})
		String severidade,

		@Schema(description = "Doenca catalogada opcional, associada somente quando aplicavel. Nulo representa uma conclusao sem vinculo obrigatorio ao catalogo de doencas.", example = "7", nullable = true)
		Long doencaId,

		@Schema(description = "Conclusao clinica livre registrada na consulta.", example = "Conduta inicial definida e retorno recomendado em sete dias.")
		String conclusao
) {
}
