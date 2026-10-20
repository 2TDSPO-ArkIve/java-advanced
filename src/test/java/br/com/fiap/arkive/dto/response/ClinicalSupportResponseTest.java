package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClinicalSupportResponseTest {

	@Test
	void leJsonPersistidoDeFontesComoLista() {
		Diagnostico diagnostico = diagnostico("[\"https://source-one.example\",\"https://source-two.example\"]");

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta(), diagnostico);

		assertEquals(50L, response.diagnosticoId());
		assertNull(response.validacaoVet());
		assertEquals(List.of(), response.referenciasRag());
		assertEquals(List.of("https://source-one.example", "https://source-two.example"), response.fontesPesquisadas());
	}

	@Test
	void refleteValidacaoPositivaPersistida() {
		Diagnostico diagnostico = diagnostico(null);
		diagnostico.setValidacaoVet("S");

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta(), diagnostico);

		assertEquals(50L, response.diagnosticoId());
		assertEquals("S", response.validacaoVet());
	}

	@Test
	void refleteValidacaoNegativaPersistida() {
		Diagnostico diagnostico = diagnostico(null);
		diagnostico.setValidacaoVet("N");

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta(), diagnostico);

		assertEquals(50L, response.diagnosticoId());
		assertEquals("N", response.validacaoVet());
	}

	@Test
	void expoeTokensPersistidosESemMetadadosRetornaNulo() {
		Diagnostico comTokens = diagnostico(null);
		comTokens.setTokensEntrada(1000L);
		comTokens.setTokensSaida(500L);
		comTokens.setTokensTotal(1500L);

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta(), comTokens);

		assertEquals(new ClinicalSupportTokenResponse(1000L, 500L, 1500L), response.tokens());
		assertNull(ClinicalSupportResponse.fromEntities(consulta(), diagnostico(null)).tokens());
	}

	@Test
	void leReferenciasRagEstruturadasSemMisturarFontesExternas() {
		Diagnostico diagnostico = diagnostico(
				"[\"https://pubmed.ncbi.nlm.nih.gov/31325462/\"]",
				"[{\"fonte\":\"ABCD\",\"titulo\":\"Feline calicivirus infection\"},"
						+ "{\"fonte\":\"CFSPH\",\"titulo\":\"Cryptococcosis\"}]"
		);

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta(), diagnostico);

		assertEquals(List.of(
				new RagReferenceResponse("ABCD", "Feline calicivirus infection"),
				new RagReferenceResponse("CFSPH", "Cryptococcosis")
		), response.referenciasRag());
		assertEquals(List.of("https://pubmed.ncbi.nlm.nih.gov/31325462/"), response.fontesPesquisadas());
	}

	@Test
	void referenciasRagPersistidasSaoSanitizadas() {
		Diagnostico diagnostico = diagnostico(
				null,
				"[null,\" Feline calicivirus infection \",\"\",\"Feline calicivirus infection\",\"Cryptococcosis\"]"
		);

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta(), diagnostico);

		assertEquals(List.of(
				new RagReferenceResponse(null, "Feline calicivirus infection"),
				new RagReferenceResponse(null, "Cryptococcosis")
		), response.referenciasRag());
	}

	@Test
	void referenciasEstruturadasSaoNormalizadasSemColapsarOrganizacoesDiferentes() {
		Diagnostico diagnostico = diagnostico(null, """
				[
				  {"fonte":" ABCD ","titulo":" Reference "},
				  {"fonte":"ABCD","titulo":"Reference"},
				  {"fonte":"OUTRA","titulo":"Reference"},
				  {"fonte":"   ","titulo":"Sem organização"},
				  {"fonte":"IGNORADA","titulo":"   "},
				  null,
				  42
				]
				""");

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta(), diagnostico);

		assertEquals(List.of(
				new RagReferenceResponse("ABCD", "Reference"),
				new RagReferenceResponse("OUTRA", "Reference"),
				new RagReferenceResponse(null, "Sem organização")
		), response.referenciasRag());
	}

	@Test
	void fontesNulasOuEmBrancoRetornamListaVazia() {
		assertEquals(List.of(), ClinicalSupportResponse.fromEntities(consulta(), diagnostico(null)).fontesPesquisadas());
		assertEquals(List.of(), ClinicalSupportResponse.fromEntities(consulta(), diagnostico("")).fontesPesquisadas());
		assertEquals(List.of(), ClinicalSupportResponse.fromEntities(consulta(), diagnostico("   ")).fontesPesquisadas());
	}

	@Test
	void registroLegadoSemPersistenciaRagRetornaListaVazia() {
		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(
				consulta(), diagnostico("[\"https://source.example\"]", null));

		assertEquals(List.of(), response.referenciasRag());
		assertEquals(List.of("https://source.example"), response.fontesPesquisadas());
	}

	@Test
	void jsonPersistidoMalformadoRetornaListaVazia() {
		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(
				consulta(), diagnostico("{", "{"));

		assertEquals(List.of(), response.fontesPesquisadas());
		assertEquals(List.of(), response.referenciasRag());
	}

	private Consulta consulta() {
		Consulta consulta = new Consulta();
		consulta.setId(63L);
		consulta.setStatus("AP");
		return consulta;
	}

	private Diagnostico diagnostico(String fontesIaJson) {
		return diagnostico(fontesIaJson, null);
	}

	private Diagnostico diagnostico(String fontesIaJson, String referenciasRagJson) {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setId(50L);
		diagnostico.setDiagnostico("Hipotese");
		diagnostico.setSeveridade("MODERADA");
		diagnostico.setInsightIa("Insight clinico");
		diagnostico.setConfianca(BigDecimal.valueOf(55));
		diagnostico.setFontesIaJson(fontesIaJson);
		diagnostico.setReferenciasRagJson(referenciasRagJson);
		return diagnostico;
	}
}
