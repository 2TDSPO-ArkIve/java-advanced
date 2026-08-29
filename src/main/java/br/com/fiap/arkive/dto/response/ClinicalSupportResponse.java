package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;

public record ClinicalSupportResponse(
		Long consultaId,
		String statusConsulta,
		String statusDescricao,
		String hipoteseDiagnostica,
		String severidadeSugerida,
		String insightClinico,
		Integer confianca
) {
	public static ClinicalSupportResponse fromEntities(Consulta consulta, Diagnostico diagnostico) {
		StatusConsulta status = StatusConsulta.fromCodigo(consulta.getStatus());
		Integer confianca = diagnostico.getConfianca() == null ? null : diagnostico.getConfianca().intValue();
		return new ClinicalSupportResponse(
				consulta.getId(),
				consulta.getStatus(),
				status.getDescricao(),
				diagnostico.getDiagnostico(),
				diagnostico.getSeveridade(),
				diagnostico.getInsightIa(),
				confianca
		);
	}
}
