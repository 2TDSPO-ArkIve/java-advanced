package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Schema(description = "Suporte clinico persistido em terminologia ArkIve. Nao expõe o contrato bruto do provedor externo.")
public record ClinicalSupportResponse(
		@Schema(description = "Identificador do diagnostico de suporte clinico gerado por IA, usado na validacao veterinaria.", example = "123")
		Long diagnosticoId,
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
		Integer confianca,
		@Schema(description = "Avaliacao de utilidade feita pelo veterinario: S para util, N para nao util e nulo quando ainda nao avaliado.", example = "S")
		String validacaoVet,
		@Schema(description = "Titulos legiveis das referencias recuperadas da base de conhecimento RAG.")
		List<String> referenciasRag,
		@Schema(description = "Fontes consultadas pelo motor clinico externo.")
		List<String> fontesPesquisadas
) {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<List<String>> FONTES_TYPE = new TypeReference<>() {
	};

	public ClinicalSupportResponse {
		referenciasRag = normalizarReferenciasRag(referenciasRag);
		fontesPesquisadas = fontesPesquisadas == null
				? List.of()
				: fontesPesquisadas.stream().filter(Objects::nonNull).toList();
	}

	public ClinicalSupportResponse(
			Long consultaId,
			String statusConsulta,
			String statusDescricao,
			String hipoteseDiagnostica,
			String severidadeSugerida,
			String insightClinico,
			Integer confianca,
			List<String> referenciasRag,
			List<String> fontesPesquisadas
	) {
		this(null, consultaId, statusConsulta, statusDescricao, hipoteseDiagnostica, severidadeSugerida,
				insightClinico, confianca, null, referenciasRag, fontesPesquisadas);
	}

	public ClinicalSupportResponse(
			Long consultaId,
			String statusConsulta,
			String statusDescricao,
			String hipoteseDiagnostica,
			String severidadeSugerida,
			String insightClinico,
			Integer confianca,
			List<String> fontesPesquisadas
	) {
		this(null, consultaId, statusConsulta, statusDescricao, hipoteseDiagnostica, severidadeSugerida,
				insightClinico, confianca, null, List.of(), fontesPesquisadas);
	}

	public ClinicalSupportResponse(
			Long consultaId,
			String statusConsulta,
			String statusDescricao,
			String hipoteseDiagnostica,
			String severidadeSugerida,
			String insightClinico,
			Integer confianca
	) {
		this(null, consultaId, statusConsulta, statusDescricao, hipoteseDiagnostica, severidadeSugerida,
				insightClinico, confianca, null, List.of(), List.of());
	}

	public static ClinicalSupportResponse fromEntities(Consulta consulta, Diagnostico diagnostico) {
		StatusConsulta status = StatusConsulta.fromCodigo(consulta.getStatus());
		Integer confianca = diagnostico.getConfianca() == null ? null : diagnostico.getConfianca().intValue();
		return new ClinicalSupportResponse(
				diagnostico.getId(),
				consulta.getId(),
				consulta.getStatus(),
				status.getDescricao(),
				diagnostico.getDiagnostico(),
				diagnostico.getSeveridade(),
				diagnostico.getInsightIa(),
				confianca,
				diagnostico.getValidacaoVet(),
				lerLista(diagnostico.getReferenciasRagJson()),
				lerLista(diagnostico.getFontesIaJson())
		);
	}

	private static List<String> lerLista(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			List<String> fontes = OBJECT_MAPPER.readValue(json, FONTES_TYPE);
			return fontes == null ? List.of() : fontes.stream().filter(Objects::nonNull).toList();
		} catch (JsonProcessingException | IllegalArgumentException ex) {
			return List.of();
		}
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
