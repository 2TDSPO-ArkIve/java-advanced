package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public self-registration payload for a veterinarian — deliberately a
 * narrower subset of {@link VeterinarioRequest} (no {@code especialidade},
 * {@code clinicaId} or {@code ativo}: an anonymous caller must not be able
 * to attach themselves to an arbitrary clinic or dictate their own active
 * flag). {@code email} is required here (unlike VeterinarioRequest, where
 * it's only enforced by {@code VeterinarioService.criar}'s business rule)
 * since it doubles as the account's login.
 */
@Schema(description = "Cadastro público de veterinário. Papel, clínica e status são definidos pelo servidor.")
public record RegistroVeterinarioRequest(
		@NotBlank
		@Size(max = 50)
		@Schema(description = "Nome do veterinário, com no máximo 50 caracteres.", example = "Dra Vera Lima")
		String nome,

		@NotBlank
		@Size(max = 10)
		@Schema(description = "CRMV com no máximo 10 caracteres.", example = "CRMV123")
		String crmv,

		@NotBlank
		@Email
		@Size(max = 200)
		@Schema(description = "E-mail usado como login e senha temporária inicial, com no máximo 200 caracteres.", example = "vera@example.com")
		String email
) {
}
