package br.com.fiap.arkive.service.clinical;

public record ClinicalSupportProviderResult(
		String diagnostico,
		String severidade,
		String insightIa,
		Integer confianca
) {
}
