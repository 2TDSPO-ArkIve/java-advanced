package br.com.fiap.arkive.dto.request;

import br.com.fiap.arkive.entity.TipoDocumentoResponsavel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration payload for a responsible person. Role, status,
 * domain type and associations are deliberately absent because they are
 * assigned by the server.
 */
@Schema(description = "Cadastro público de uma pessoa responsável. O papel e o status são definidos pelo servidor.")
public record RegistroResponsavelRequest(
		@NotBlank
		@Size(max = 50)
		@Schema(description = "Nome da pessoa responsável, com no máximo 50 caracteres.", example = "Maria Silva")
		String nome,

		@NotNull(message = "Tipo de documento é obrigatório.")
		@Schema(description = "Tipo do documento submetido. Valores aceitos: CPF ou RG.", example = "CPF")
		TipoDocumentoResponsavel tipoDocumento,

		@NotBlank
		@Size(max = 20)
		@Schema(description = "CPF ou RG. CPF pode vir formatado e será persistido somente com dígitos; RG aceita até 14 caracteres alfanuméricos após normalização.", example = "529.982.247-25")
		String documento,

		@NotBlank
		@Email
		@Size(max = 200)
		@Schema(description = "E-mail usado como login, com no máximo 200 caracteres.", example = "maria@example.com")
		String email,

		@Size(max = 20)
		@Schema(description = "Telefone opcional, com no máximo 20 caracteres.", example = "(11) 99999-9999")
		String telefone,

		@NotBlank
		@Size(min = 8, max = 72)
		@Schema(description = "Senha entre 8 e 72 caracteres, sujeita à política de complexidade.", example = "SenhaSegura1")
		String senha,

		@NotBlank
		@Size(min = 8, max = 72)
		@Schema(description = "Confirmação idêntica à senha.", example = "SenhaSegura1")
		String confirmacaoSenha
) {
}
