package br.com.fiap.arkive.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BASIC_AUTH = "basicAuth";

	@Bean
	public OpenAPI arkiveOpenAPI() {
		return new OpenAPI()
				.components(new Components()
						.addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("basic")
								.description("HTTP Basic com usuarios ArkIve autenticados pelo Spring Security.")))
				.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
				.info(new Info()
						.title("ArkIve API")
						.description("API REST para a jornada clinica veterinaria do ArkIve. O suporte por IA oferece apoio investigativo e nao substitui o julgamento nem a confirmacao do veterinario.")
						.version("0.0.1-SNAPSHOT"))
				.addTagsItem(new Tag().name("Consultas").description("Cadastro, leitura e fluxo clinico das consultas."))
				.addTagsItem(new Tag().name("Suporte Clinico").description("Suporte clinico por IA, sempre provisorio e solicitado por veterinario."))
				.addTagsItem(new Tag().name("Diagnosticos").description("Diagnosticos e conclusoes clinicas dentro do escopo autorizado."))
				.addTagsItem(new Tag().name("Prescricoes").description("Prescricoes criadas exclusivamente por veterinarios apos consulta finalizada."))
				.addTagsItem(new Tag().name("Adesao").description("Registro de adesao terapeutica pelo responsavel vinculado ao animal."))
				.addTagsItem(new Tag().name("Animais").description("Animais acompanhados pelo ArkIve."))
				.addTagsItem(new Tag().name("Veterinarios").description("Profissionais veterinarios."))
				.addTagsItem(new Tag().name("Responsaveis").description("Responsaveis por animais."))
				.addTagsItem(new Tag().name("Clinicas").description("Clinicas e contexto administrativo."));
	}

}
