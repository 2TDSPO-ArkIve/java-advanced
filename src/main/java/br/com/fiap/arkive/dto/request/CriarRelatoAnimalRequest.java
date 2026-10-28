package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CriarRelatoAnimalRequest(
		@NotBlank(message = "Texto do relato e obrigatorio.")
		@Size(max = 2000, message = "Texto do relato deve ter no maximo 2000 caracteres.")
		String texto,
		LocalDate dataOcorrencia,
		@NotNull(message = "Origem de transcricao e obrigatoria.")
		Boolean originadoDeTranscricao
) {
}
