package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EventoPreventivoRequest(
		LocalDate dataAplicacao,

		@NotNull
		LocalDate dataProximo,

		@Size(max = 20)
		String status,

		@Size(max = 1)
		String alerta,

		@Size(max = 300)
		String observacao,

		@NotNull
		Long animalId,

		@NotNull
		Long protocoloId,

		Long consultaId
) {
}
