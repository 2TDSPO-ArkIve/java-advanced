package br.com.fiap.arkive.service.clinical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public record ClinicalSupportProviderResult(
		String diagnostico,
		String severidade,
		String insightIa,
		Integer confianca,
		List<String> fontesPesquisadas,
		List<RagReference> referenciasRag,
		ClinicalSupportTokenUsage tokens
) {
	public ClinicalSupportProviderResult {
		fontesPesquisadas = fontesPesquisadas == null
				? List.of()
				: Collections.unmodifiableList(new ArrayList<>(fontesPesquisadas));
		referenciasRag = normalizarReferenciasRag(referenciasRag);
	}

	public ClinicalSupportProviderResult(
			String diagnostico,
			String severidade,
			String insightIa,
			Integer confianca,
			List<String> fontesPesquisadas
	) {
		this(diagnostico, severidade, insightIa, confianca, fontesPesquisadas, List.of(), null);
	}

	public ClinicalSupportProviderResult(
			String diagnostico,
			String severidade,
			String insightIa,
			Integer confianca,
			List<String> fontesPesquisadas,
			List<RagReference> referenciasRag
	) {
		this(diagnostico, severidade, insightIa, confianca, fontesPesquisadas, referenciasRag, null);
	}

	public ClinicalSupportProviderResult(String diagnostico, String severidade, String insightIa, Integer confianca) {
		this(diagnostico, severidade, insightIa, confianca, List.of(), List.of(), null);
	}

	private static List<RagReference> normalizarReferenciasRag(List<RagReference> referenciasRag) {
		if (referenciasRag == null || referenciasRag.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<RagReference> referencias = new LinkedHashSet<>();
		for (RagReference referencia : referenciasRag) {
			if (referencia != null && referencia.titulo() != null && !referencia.titulo().isBlank()) {
				referencias.add(new RagReference(referencia.fonte(), referencia.titulo()));
			}
		}
		return List.copyOf(referencias);
	}
}
