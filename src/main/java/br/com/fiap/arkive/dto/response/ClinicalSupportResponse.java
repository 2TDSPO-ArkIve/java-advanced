package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Suporte clinico persistido em terminologia ArkIve. Nao expõe o contrato bruto do provedor externo.")
public record ClinicalSupportResponse(
		@Schema(example = "42")
		Long consultaId,
		@Schema(description = "Status atual da consulta.", example = "AP")
		String statusConsulta,
		@Schema(example = "Aguardando Parecer")
		String statusDescricao,
		@Schema(description = "Hipotese diagnostica gerada como apoio clinico, ainda nao confirmada.", example = "Otite externa")
		String hipoteseDiagnostica,
		@Schema(example = "MODERADA")
		String severidadeSugerida,
		@Schema(description = "Insight clinico investigativo retornado pelo motor externo.", example = "Avaliar ouvido externo e historico de prurido.")
		String insightClinico,
		@Schema(description = "Confianca informada pelo motor clinico em percentual.", example = "65")
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
