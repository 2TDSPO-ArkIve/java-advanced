package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Diagnostico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadados opcionais de consumo de tokens informados pelo motor clinico.")
public record ClinicalSupportTokenResponse(
		@Schema(description = "Tokens de entrada.", example = "1000", nullable = true)
		Long entrada,
		@Schema(description = "Tokens de saida.", example = "500", nullable = true)
		Long saida,
		@Schema(description = "Total de tokens.", example = "1500", nullable = true)
		Long total
) {
	public static ClinicalSupportTokenResponse fromEntity(Diagnostico diagnostico) {
		if (diagnostico.getTokensEntrada() == null
				&& diagnostico.getTokensSaida() == null
				&& diagnostico.getTokensTotal() == null) {
			return null;
		}
		return new ClinicalSupportTokenResponse(
				diagnostico.getTokensEntrada(),
				diagnostico.getTokensSaida(),
				diagnostico.getTokensTotal()
		);
	}
}
