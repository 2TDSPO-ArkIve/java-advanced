package br.com.fiap.arkive.service.clinical;

import br.com.fiap.arkive.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RenderClinicalSupportProviderTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void mapeiaContratoSnakeCaseCompletoDoRender() throws Exception {
		RenderClinicalEngineResponse response = objectMapper.readValue(fixture(), RenderClinicalEngineResponse.class);

		assertEquals("Suspeita de Displasia Coxofemoral em Golden Retriever", response.diagnostico());
		assertEquals("MODERADA", response.severidade());
		assertEquals(65, response.confianca());
		assertEquals(new ClinicalSupportTokenUsage(1000L, 500L, 1500L), response.tokens());
		assertEquals(List.of(
				new RagReference("ABCD", "Feline calicivirus infection"),
				new RagReference("AAZV", "Herpesviridae · Alpha · Varicellovirus · Felid alphaherpesvirus 1"),
				new RagReference("CFSPH", "Cryptococcosis")
		), response.fontesRag());
		assertEquals(fontesPesquisadasFixture(), response.fontesPesquisadas());
	}

	@Test
	void toProviderResultPreservaFontesPesquisadas() throws Exception {
		RenderClinicalEngineResponse response = objectMapper.readValue(fixture(), RenderClinicalEngineResponse.class);

		ClinicalSupportProviderResult result = response.toProviderResult();

		assertEquals(List.of(
				new RagReference("ABCD", "Feline calicivirus infection"),
				new RagReference("AAZV", "Herpesviridae · Alpha · Varicellovirus · Felid alphaherpesvirus 1"),
				new RagReference("CFSPH", "Cryptococcosis")
		), result.referenciasRag());
		assertEquals(fontesPesquisadasFixture(), result.fontesPesquisadas());
		assertEquals(new ClinicalSupportTokenUsage(1000L, 500L, 1500L), result.tokens());
	}

	@Test
	void fontesPesquisadasVaziasMapeiamComoListaVazia() throws Exception {
		RenderClinicalEngineResponse response = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65,
				  "fontes_pesquisadas": []
				}
				""", RenderClinicalEngineResponse.class);

		assertEquals(List.of(), response.fontesPesquisadas());
		assertEquals(List.of(), response.toProviderResult().fontesPesquisadas());
	}

	@Test
	void fontesPesquisadasAusentesOuNulasMapeiamComoListaVazia() throws Exception {
		RenderClinicalEngineResponse semCampo = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65
				}
				""", RenderClinicalEngineResponse.class);
		RenderClinicalEngineResponse campoNulo = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65,
				  "fontes_pesquisadas": null
				}
				""", RenderClinicalEngineResponse.class);

		assertEquals(List.of(), semCampo.fontesPesquisadas());
		assertEquals(List.of(), campoNulo.fontesPesquisadas());
	}

	@Test
	void tokensAusentesSaoMantidosComoNulosParaCompatibilidade() throws Exception {
		RenderClinicalEngineResponse response = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "AUSENTE",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65
				}
				""", RenderClinicalEngineResponse.class);

		assertEquals("AUSENTE", response.toProviderResult().severidade());
		assertEquals(null, response.tokens());
		assertEquals(null, response.toProviderResult().tokens());
	}

	@Test
	void rejeitaTokenNegativoSemRejeitarMetadadosAusentes() {
		RenderClinicalSupportProvider provider = providerRespondendo("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "AUSENTE",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65,
				  "tokens": {"entrada": -1, "saida": 2, "total": 1}
				}
				""");

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
	}

	@Test
	void fontesRagAusentesOuNulasMapeiamComoListaVazia() throws Exception {
		RenderClinicalEngineResponse semCampo = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65
				}
				""", RenderClinicalEngineResponse.class);
		RenderClinicalEngineResponse campoNulo = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65,
				  "fontes_rag": null
				}
				""", RenderClinicalEngineResponse.class);
		RenderClinicalEngineResponse campoVazio = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65,
				  "fontes_rag": []
				}
				""", RenderClinicalEngineResponse.class);

		assertEquals(List.of(), semCampo.fontesRag());
		assertEquals(List.of(), semCampo.toProviderResult().referenciasRag());
		assertEquals(List.of(), campoNulo.fontesRag());
		assertEquals(List.of(), campoNulo.toProviderResult().referenciasRag());
		assertEquals(List.of(), campoVazio.fontesRag());
		assertEquals(List.of(), campoVazio.toProviderResult().referenciasRag());
	}

	@Test
	void ignoraEntradasRagInvalidasTitulosEmBrancoEDuplicadosPreservandoOrdem() throws Exception {
		RenderClinicalEngineResponse response = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65,
				  "fontes_rag": [
				    null,
				    "entrada-invalida",
				    {"fonte":"ABCD","titulo":" Feline calicivirus infection "},
				    {"fonte":"ABCD","titulo":"Feline calicivirus infection"},
				    {"fonte":"OUTRA","titulo":"Feline calicivirus infection"},
				    {"fonte":"   ","titulo":"Reference without organization"},
				    {"fonte":"AAZV","titulo":"   "},
				    {"fonte":"CFSPH"},
				    {"fonte":"CFSPH","titulo":"Cryptococcosis"},
				    {"fonte":"INVALIDA","titulo":42}
				  ],
				  "fontes_pesquisadas": ["https://pubmed.ncbi.nlm.nih.gov/31325462/"]
				}
				""", RenderClinicalEngineResponse.class);

		ClinicalSupportProviderResult result = response.toProviderResult();

		assertEquals(List.of(
				new RagReference("ABCD", "Feline calicivirus infection"),
				new RagReference("OUTRA", "Feline calicivirus infection"),
				new RagReference(null, "Reference without organization"),
				new RagReference("CFSPH", "Cryptococcosis")
		), result.referenciasRag());
		assertEquals(List.of("https://pubmed.ncbi.nlm.nih.gov/31325462/"), result.fontesPesquisadas());
	}

	@Test
	void chamaEndpointConfiguradoEUsoContratoValido() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://engine.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RenderClinicalSupportProvider provider = new RenderClinicalSupportProvider(builder.build());
		server.expect(requestTo("https://engine.test/diagnostico/6"))
				.andRespond(withSuccess(fixture(), MediaType.APPLICATION_JSON));

		ClinicalSupportProviderResult result = provider.gerarSuporte(6L);

		assertEquals("Suspeita de Displasia Coxofemoral em Golden Retriever", result.diagnostico());
		assertEquals("MODERADA", result.severidade());
		assertEquals(65, result.confianca());
		assertEquals(new ClinicalSupportTokenUsage(1000L, 500L, 1500L), result.tokens());
		assertEquals(List.of(
				new RagReference("ABCD", "Feline calicivirus infection"),
				new RagReference("AAZV", "Herpesviridae · Alpha · Varicellovirus · Felid alphaherpesvirus 1"),
				new RagReference("CFSPH", "Cryptococcosis")
		), result.referenciasRag());
		assertEquals(fontesPesquisadasFixture(), result.fontesPesquisadas());
		server.verify();
	}

	@Test
	void rejeitaPayloadMalformado() {
		RenderClinicalSupportProvider provider = providerRespondendo("""
				{
				  "ds_diagnostico": "",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Texto",
				  "pc_confianca": 65,
				  "fontes_pesquisadas": []
				}
				""");

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
	}

	@Test
	void traduzJsonInvalidoComoRespostaInvalida() {
		RenderClinicalSupportProvider provider = providerRespondendo("{");

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
	}

	@Test
	void traduzLimiteDeQuota() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://engine.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RenderClinicalSupportProvider provider = new RenderClinicalSupportProvider(builder.build());
		server.expect(requestTo("https://engine.test/diagnostico/6"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
	}

	@Test
	void traduzErroServidorExterno() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://engine.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RenderClinicalSupportProvider provider = new RenderClinicalSupportProvider(builder.build());
		server.expect(requestTo("https://engine.test/diagnostico/6"))
				.andRespond(withServerError());

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
	}

	private RenderClinicalSupportProvider providerRespondendo(String body) {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://engine.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://engine.test/diagnostico/6"))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
		return new RenderClinicalSupportProvider(builder.build());
	}

	@Test
	void traduzTimeoutOuFalhaDeConexaoSemExporInfraestrutura() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://engine.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RenderClinicalSupportProvider provider = new RenderClinicalSupportProvider(builder.build());
		server.expect(requestTo("https://engine.test/diagnostico/6"))
				.andRespond(request -> {
					throw new org.springframework.web.client.ResourceAccessException("read timed out");
				});

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
		assertEquals("Motor clinico temporariamente indisponivel.", exception.getMessage());
	}

	private List<String> fontesPesquisadasFixture() {
		return List.of(
				"https://pubmed.ncbi.nlm.nih.gov/31325462/",
				"https://pubmed.ncbi.nlm.nih.gov/35232714/",
				"https://pubmed.ncbi.nlm.nih.gov/35632680/"
		);
	}

	private String fixture() {
		try (var inputStream = Objects.requireNonNull(
				getClass().getResourceAsStream("/fixtures/clinical-engine/diagnostico-success.json"),
				"Fixture de suporte clinico nao encontrada."
		)) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			throw new IllegalStateException("Fixture de suporte clinico indisponivel.", ex);
		}
	}
}
