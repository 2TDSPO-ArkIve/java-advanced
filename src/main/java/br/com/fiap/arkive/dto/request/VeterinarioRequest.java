package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VeterinarioRequest(
		@NotBlank
		@Size(max = 150)
		String nome,

		@NotBlank
		@Size(max = 20)
		String crmv,

		@Size(max = 100)
		String especialidade,

		@Email
		@Size(max = 200)
		String email,

		Long clinicaId,

		@Size(max = 1)
		String ativo
) {
}
