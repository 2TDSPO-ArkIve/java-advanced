package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProtocoloPreventivoRequest(
		@NotBlank
		@Size(max = 50)
		String nome,

		@NotBlank
		@Size(max = 50)
		String tipo,

		String descricao,

		@NotNull
		@Positive
		Integer intervalo,

		@NotNull
		@Min(0)
		Integer idadeMin,

		Long especieId,

		Long racaId,

		@Size(max = 1)
		String ativo
) {
}
