package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PrescricaoRequest(
		@NotBlank
		@Size(max = 150)
		String medicamento,

		@NotBlank
		@Size(max = 50)
		String dosagem,

		@Size(max = 100)
		String frequencia,

		@Size(max = 50)
		String viaAdministracao,

		@NotNull
		LocalDate dataInicio,

		LocalDate dataFim,

		String instrucoes,

		@NotNull
		Long consultaId
) {
}
