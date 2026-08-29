package br.com.fiap.arkive.service.clinical;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record RenderClinicalEngineResponse(
		@JsonProperty("ds_diagnostico")
		String diagnostico,

		@JsonProperty("tp_severidade")
		String severidade,

		@JsonProperty("ds_insight_ia")
		String insightIa,

		@JsonProperty("pc_confianca")
		Integer confianca,

		@JsonProperty("fontes_pesquisadas")
		JsonNode fontesPesquisadas
) {
	public ClinicalSupportProviderResult toProviderResult() {
		return new ClinicalSupportProviderResult(diagnostico, severidade, insightIa, confianca);
	}
}
