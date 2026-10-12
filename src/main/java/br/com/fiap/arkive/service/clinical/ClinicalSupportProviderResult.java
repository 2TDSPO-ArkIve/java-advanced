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
		List<String> referenciasRag
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
		this(diagnostico, severidade, insightIa, confianca, fontesPesquisadas, List.of());
	}

	public ClinicalSupportProviderResult(String diagnostico, String severidade, String insightIa, Integer confianca) {
		this(diagnostico, severidade, insightIa, confianca, List.of(), List.of());
	}

	private static List<String> normalizarReferenciasRag(List<String> referenciasRag) {
		if (referenciasRag == null || referenciasRag.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<String> titulos = new LinkedHashSet<>();
		for (String titulo : referenciasRag) {
			if (titulo != null && !titulo.isBlank()) {
				titulos.add(titulo.strip());
			}
		}
		return List.copyOf(titulos);
	}
}
