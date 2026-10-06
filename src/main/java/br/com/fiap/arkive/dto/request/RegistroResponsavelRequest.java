package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration payload for a responsible person. Role, status,
 * domain type and associations are deliberately absent because they are
 * assigned by the server.
 */
public record RegistroResponsavelRequest(
		@NotBlank
		@Size(max = 50)
		String nome,

		@NotBlank
		@Size(max = 20)
		String documento,

		@NotBlank
		@Email
		@Size(max = 200)
		String email,

		@Size(max = 20)
		String telefone,

		@NotBlank
		@Size(min = 8, max = 72)
		String senha,

		@NotBlank
		@Size(min = 8, max = 72)
		String confirmacaoSenha
) {
}
