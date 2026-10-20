package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.ArrayList;
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
		@Schema(description = "Severidade sugerida. AUSENTE e diferente de nulo, que significa nao informado.", example = "MODERADA",
				allowableValues = {"AUSENTE", "LEVE", "MODERADA", "GRAVE"})
		String severidadeSugerida,
		@Schema(description = "Insight clinico investigativo retornado pelo motor externo.", example = "Avaliar ouvido externo e historico de prurido.")
		String insightClinico,
		@Schema(description = "Confianca informada pelo motor clinico em percentual.", example = "65")
		Integer confianca,
		@Schema(description = "Avaliacao de utilidade feita pelo veterinario: S para util, N para nao util e nulo quando ainda nao avaliado.", example = "S")
		String validacaoVet,
		@Schema(description = "Metadados opcionais de tokens informados pelo motor clinico; nulo quando ausentes na resposta original.", nullable = true)
		ClinicalSupportTokenResponse tokens,
		@Schema(description = "Organização de origem e título das referências recuperadas da base de conhecimento RAG.")
		List<RagReferenceResponse> referenciasRag,
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
			List<RagReferenceResponse> referenciasRag,
			List<String> fontesPesquisadas
	) {
		this(null, consultaId, statusConsulta, statusDescricao, hipoteseDiagnostica, severidadeSugerida,
				insightClinico, confianca, null, null, referenciasRag, fontesPesquisadas);
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
				insightClinico, confianca, null, null, List.of(), fontesPesquisadas);
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
				insightClinico, confianca, null, null, List.of(), List.of());
	}

	public ClinicalSupportResponse(
			Long diagnosticoId,
			Long consultaId,
			String statusConsulta,
			String statusDescricao,
			String hipoteseDiagnostica,
			String severidadeSugerida,
			String insightClinico,
			Integer confianca,
			String validacaoVet,
			List<RagReferenceResponse> referenciasRag,
			List<String> fontesPesquisadas
	) {
		this(diagnosticoId, consultaId, statusConsulta, statusDescricao, hipoteseDiagnostica,
				severidadeSugerida, insightClinico, confianca, validacaoVet, null,
				referenciasRag, fontesPesquisadas);
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
				ClinicalSupportTokenResponse.fromEntity(diagnostico),
				lerReferenciasRag(diagnostico.getReferenciasRagJson()),
				lerListaTexto(diagnostico.getFontesIaJson())
		);
	}

	private static List<String> lerListaTexto(String json) {
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

	private static List<RagReferenceResponse> lerReferenciasRag(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			JsonNode root = OBJECT_MAPPER.readTree(json);
			if (root == null || !root.isArray()) {
				return List.of();
			}
			List<RagReferenceResponse> referencias = new ArrayList<>();
			for (JsonNode entry : root) {
				if (entry.isTextual()) {
					referencias.add(new RagReferenceResponse(null, entry.textValue()));
			} else if (entry.isObject()) {
					referencias.add(new RagReferenceResponse(
						texto(entry.get("fonte")), texto(entry.get("titulo"))));
			}
			}
			return normalizarReferenciasRag(referencias);
		} catch (JsonProcessingException | IllegalArgumentException ex) {
			return List.of();
		}
	}

	private static String texto(JsonNode node) {
		return node != null && node.isTextual() ? node.textValue() : null;
	}

	private static List<RagReferenceResponse> normalizarReferenciasRag(
			List<RagReferenceResponse> referenciasRag
	) {
		if (referenciasRag == null || referenciasRag.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<RagReferenceResponse> referencias = new LinkedHashSet<>();
		for (RagReferenceResponse referencia : referenciasRag) {
			if (referencia != null && referencia.titulo() != null && !referencia.titulo().isBlank()) {
				referencias.add(new RagReferenceResponse(referencia.fonte(), referencia.titulo()));
			}
		}
		return List.copyOf(referencias);
	}
}
