package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnimalRequest(
		@NotBlank
		@Size(max = 100)
		String nome,

		LocalDate dataNascimento,

		@Positive
		BigDecimal pesoKg,

		@NotNull
		Long especieId,

		Long racaId,

		Long clinicaCadastroId,

		@Size(max = 1)
		String ativo
) {
}
