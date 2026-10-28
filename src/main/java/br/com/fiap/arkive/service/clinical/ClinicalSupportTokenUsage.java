package br.com.fiap.arkive.service.clinical;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClinicalSupportTokenUsage(
		@JsonProperty("entrada") Long entrada,
		@JsonProperty("saida") Long saida,
		@JsonProperty("total") Long total
) {
	public boolean possuiValorNegativo() {
		return negativo(entrada) || negativo(saida) || negativo(total);
	}

	private static boolean negativo(Long valor) {
		return valor != null && valor < 0;
	}
}
