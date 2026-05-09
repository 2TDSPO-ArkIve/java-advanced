package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicaRequest(
		@NotBlank
		@Size(max = 200)
		String nome,

		@NotBlank
		@Size(max = 18)
		String cnpj,

		@Size(max = 300)
		String endereco,

		@Size(max = 20)
		String telefone,

		@Email
		@Size(max = 200)
		String email,

		@Size(max = 1)
		String ativo
) {
}
