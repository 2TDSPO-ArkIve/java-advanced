package br.com.fiap.arkive.service.clinical;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		List<String> fontesPesquisadas,

		@JsonProperty("fontes_rag")
		List<RagReference> fontesRag
) {
	public RenderClinicalEngineResponse {
		fontesPesquisadas = fontesPesquisadas == null
				? List.of()
				: Collections.unmodifiableList(new ArrayList<>(fontesPesquisadas));
		fontesRag = fontesRag == null
				? List.of()
				: Collections.unmodifiableList(new ArrayList<>(fontesRag));
	}

	public ClinicalSupportProviderResult toProviderResult() {
		return new ClinicalSupportProviderResult(
				diagnostico,
				severidade,
				insightIa,
				confianca,
				fontesPesquisadas,
				fontesRag
		);
	}
}
