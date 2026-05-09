package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Diagnostico;

import java.math.BigDecimal;

public record DiagnosticoResponse(
		Long id,
		String diagnostico,
		String severidade,
		String confirmado,
		String insightIa,
		BigDecimal confianca,
		String validacaoVet,
		Long consultaId,
		Long doencaId
) {
	public static DiagnosticoResponse fromEntity(Diagnostico diagnostico) {
		Long doencaId = diagnostico.getDoenca() == null ? null : diagnostico.getDoenca().getId();
		return new DiagnosticoResponse(
				diagnostico.getId(),
				diagnostico.getDiagnostico(),
				diagnostico.getSeveridade(),
				diagnostico.getConfirmado(),
				diagnostico.getInsightIa(),
				diagnostico.getConfianca(),
				diagnostico.getValidacaoVet(),
				diagnostico.getConsulta().getId(),
				doencaId
		);
	}
}
