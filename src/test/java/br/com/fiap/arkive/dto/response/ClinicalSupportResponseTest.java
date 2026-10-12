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

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico);

		assertEquals(50L, response.diagnosticoId());
		assertNull(response.validacaoVet());
		assertEquals(List.of(), response.referenciasRag());
		assertEquals(List.of("https://source-one.example", "https://source-two.example"), response.fontesPesquisadas());
	}

	@Test
	void refleteValidacaoPositivaPersistida() {
		Diagnostico diagnostico = diagnostico(null);
		diagnostico.setValidacaoVet("S");

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico);

		assertEquals(50L, response.diagnosticoId());
		assertEquals("S", response.validacaoVet());
	}

	@Test
	void refleteValidacaoNegativaPersistida() {
		Diagnostico diagnostico = diagnostico(null);
		diagnostico.setValidacaoVet("N");

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico);

		assertEquals(50L, response.diagnosticoId());
		assertEquals("N", response.validacaoVet());
	}

	@Test
	void leReferenciasRagPersistidasSemMisturarFontesExternas() {
		Diagnostico diagnostico = diagnostico(
				"[\"https://pubmed.ncbi.nlm.nih.gov/31325462/\"]",
				"[\"Feline calicivirus infection\",\"Cryptococcosis\"]"
		);

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico);

		assertEquals(List.of("Feline calicivirus infection", "Cryptococcosis"), response.referenciasRag());
		assertEquals(List.of("https://pubmed.ncbi.nlm.nih.gov/31325462/"), response.fontesPesquisadas());
	}

	@Test
	void referenciasRagPersistidasSaoSanitizadas() {
		Diagnostico diagnostico = diagnostico(
				null,
				"[null,\" Feline calicivirus infection \",\"\",\"Feline calicivirus infection\",\"Cryptococcosis\"]"
		);

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico);

		assertEquals(List.of("Feline calicivirus infection", "Cryptococcosis"), response.referenciasRag());
	}

	@Test
	void fontesNulasOuEmBrancoRetornamListaVazia() {
		assertEquals(List.of(), ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico(null)).fontesPesquisadas());
		assertEquals(List.of(), ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico("")).fontesPesquisadas());
		assertEquals(List.of(), ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico("   ")).fontesPesquisadas());
	}

	@Test
	void registroLegadoSemPersistenciaRagRetornaListaVazia() {
		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(
				consulta("AP"), diagnostico("[\"https://source.example\"]", null));

		assertEquals(List.of(), response.referenciasRag());
		assertEquals(List.of("https://source.example"), response.fontesPesquisadas());
	}

	@Test
	void jsonPersistidoMalformadoRetornaListaVazia() {
		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico("{"));

		assertEquals(List.of(), response.fontesPesquisadas());
	}

	private Consulta consulta(String status) {
		Consulta consulta = new Consulta();
		consulta.setId(63L);
		consulta.setStatus(status);
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
