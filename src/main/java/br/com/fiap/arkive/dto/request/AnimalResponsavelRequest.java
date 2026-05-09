package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AnimalResponsavelRequest(
		@NotNull
		Long animalId,

		@NotNull
		Long responsavelId,

		@NotBlank
		@Size(max = 40)
		String tipoVinculo,

		LocalDate dataInicio,

		LocalDate dataFim,

		@Size(max = 1)
		String principal,

		@Size(max = 1)
		String ativo
) {
}
