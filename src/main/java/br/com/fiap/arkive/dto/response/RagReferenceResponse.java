package br.com.fiap.arkive.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Referência recuperada da base de conhecimento ArkIve.")
public record RagReferenceResponse(
		@Schema(description = "Identificador da organização fornecido pelo motor clínico.", example = "ABCD")
		String fonte,
		@Schema(description = "Título legível do documento ou artigo.", example = "Feline calicivirus infection")
		String titulo
) {
	public RagReferenceResponse {
		fonte = fonte == null || fonte.isBlank() ? null : fonte.strip();
		titulo = titulo == null ? null : titulo.strip();
	}
}
