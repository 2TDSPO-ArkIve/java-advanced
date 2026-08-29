package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarAdesaoPrescricaoRequest(
		@NotNull
		Long prescricaoId,

		@NotBlank
		@Size(max = 1)
		String tomou,

		String observacao
) {
}
