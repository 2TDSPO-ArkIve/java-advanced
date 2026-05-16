package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnimalRequest(
		@NotBlank
		@Size(max = 100)
		String nome,

		@NotNull
		Long especieId,

		Long racaId,

		@Size(max = 1)
		String sexo,

		@Size(max = 1)
		String castrado,

		Long clinicaId,

		@Size(max = 1)
		String ativo
) {
}
