package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.dto.response.AdesaoPrescricaoResponse;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.dto.response.EspecieResponse;
import br.com.fiap.arkive.dto.response.PrescricaoResponse;
import br.com.fiap.arkive.dto.response.RacaResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AdesaoPrescricaoService;
import br.com.fiap.arkive.service.AnimalService;
import br.com.fiap.arkive.service.ConsultaService;
import br.com.fiap.arkive.service.EspecieService;
import br.com.fiap.arkive.service.PasswordLifecycleService;
import br.com.fiap.arkive.service.PrescricaoService;
import br.com.fiap.arkive.service.RacaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-nodb")
class AdminClinicaControllerTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private AnimalService animalService;

	@MockitoBean
	private ConsultaService consultaService;

	@MockitoBean
	private PrescricaoService prescricaoService;

	@MockitoBean
	private AdesaoPrescricaoService adesaoPrescricaoService;

	@MockitoBean
	private EspecieService especieService;

	@MockitoBean
	private RacaService racaService;

	@MockitoBean
	private PasswordLifecycleService passwordLifecycleService;

	@Autowired
	AdminClinicaControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@BeforeEach
	void setUp() {
		when(animalService.listarAutorizado(any(), any(), any(), any(), any(), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(animal())));
		when(consultaService.listarAutorizado(any(), any(), any(), any(), any(), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(consulta())));
		when(prescricaoService.listarAutorizado(any(), any(), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(prescricao())));
		when(adesaoPrescricaoService.listarAutorizado(any(), any(), any(), any(), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(adesao())));
		when(animalService.buscarPorIdAutorizado(eq(1L), any())).thenReturn(animal());
		when(consultaService.buscarPorIdAutorizado(eq(10L), any())).thenReturn(consulta());
		when(prescricaoService.buscarPorIdAutorizado(eq(20L), any())).thenReturn(prescricao());
		when(adesaoPrescricaoService.buscarPorIdAutorizado(eq(30L), any())).thenReturn(adesao());
		when(especieService.listar(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(especie())));
		when(racaService.listar(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(raca())));
	}

	@Test
	void adminClinicaSidebarIncluiNavegacaoOperacional() throws Exception {
		mockMvc.perform(get("/admin/dashboard").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/admin/dashboard")))
				.andExpect(content().string(containsString("/admin/animais")))
				.andExpect(content().string(containsString("/admin/consultas")))
				.andExpect(content().string(containsString("/admin/prescricoes")))
				.andExpect(content().string(containsString("/admin/adesoes")))
				.andExpect(content().string(not(containsString("/sysadmin/usuarios"))));
	}

	@Test
	void dashboardAdminClinicaRenderizaAcessosEContagensReais() throws Exception {
		mockMvc.perform(get("/admin/dashboard").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Administração da Clínica")))
				.andExpect(content().string(containsString("Cadastro e manutenção dos animais da clínica.")))
				.andExpect(content().string(containsString("Acompanhamento de consultas no escopo da clínica.")))
				.andExpect(content().string(containsString("Consulta de prescrições registradas")))
				.andExpect(content().string(containsString("Consulta de registros de adesão terapêutica.")));
	}

	@Test
	void paginasClinicScopedRenderizam() throws Exception {
		mockMvc.perform(get("/admin/animais").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Rex")))
				.andExpect(content().string(containsString("Novo animal")));

		mockMvc.perform(get("/admin/consultas").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Rex")))
				.andExpect(content().string(containsString("Dra. Vera")));

		mockMvc.perform(get("/admin/prescricoes").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Meloxicam")))
				.andExpect(content().string(containsString("Ver")));

		mockMvc.perform(get("/admin/adesoes").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Rui Tutor")))
				.andExpect(content().string(containsString("Sim")));
	}

	@Test
	void detalhesClinicosSaoReadOnly() throws Exception {
		mockMvc.perform(get("/admin/consultas/10").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Consulta anual")))
				.andExpect(content().string(not(containsString("Iniciar"))))
				.andExpect(content().string(not(containsString("Narrativa"))))
				.andExpect(content().string(not(containsString("Suporte clínico"))))
				.andExpect(content().string(not(containsString("Finalizar"))))
				.andExpect(content().string(not(containsString("Cancelar"))))
				.andExpect(content().string(not(containsString("Editar"))))
				.andExpect(content().string(not(containsString("Excluir"))));

		mockMvc.perform(get("/admin/prescricoes/20").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Meloxicam")))
				.andExpect(content().string(not(containsString("Cadastrar"))))
				.andExpect(content().string(not(containsString("Editar"))))
				.andExpect(content().string(not(containsString("Excluir"))));

		mockMvc.perform(get("/admin/adesoes/30").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Dose administrada")))
				.andExpect(content().string(not(containsString("Cadastrar"))))
				.andExpect(content().string(not(containsString("Editar"))))
				.andExpect(content().string(not(containsString("Excluir"))));
	}

	@Test
	void animaisUsamServicoPrincipalAwareParaEscrita() throws Exception {
		ArgumentCaptor<UsuarioPrincipal> principalCaptor = ArgumentCaptor.forClass(UsuarioPrincipal.class);

		mockMvc.perform(post("/admin/animais")
						.with(user(adminPrincipal()))
						.with(csrf())
						.param("nome", "Luna")
						.param("especieId", "1")
						.param("racaId", "2")
						.param("sexo", "F")
						.param("castrado", "S"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/animais"));

		verify(animalService).criar(any(), principalCaptor.capture());
		assertEquals(1L, principalCaptor.getValue().getClinicaId());
	}

	@Test
	void consultasPrescricoesEAdesoesNaoTemRotasWebDePost() throws Exception {
		mockMvc.perform(post("/admin/consultas/10").with(user(adminPrincipal())).with(csrf()))
				.andExpect(status().is4xxClientError());
		mockMvc.perform(post("/admin/prescricoes/20").with(user(adminPrincipal())).with(csrf()))
				.andExpect(status().is4xxClientError());
		mockMvc.perform(post("/admin/adesoes/30").with(user(adminPrincipal())).with(csrf()))
				.andExpect(status().is4xxClientError());

		verify(consultaService, never()).atualizar(any(), any(), any());
		verify(prescricaoService, never()).atualizar(any(), any(), any());
		verify(adesaoPrescricaoService, never()).registrar(any(), any());
	}

	@Test
	@WithMockUser(roles = "VETERINARIO")
	void veterinarioNaoAcessaAdminClinica() throws Exception {
		mockMvc.perform(get("/admin/animais"))
				.andExpect(status().isForbidden());
	}

	private UsuarioPrincipal adminPrincipal() {
		return new UsuarioPrincipal(
				5L,
				"Clara Admin",
				"clara@arkive.com",
				"$2a$10$hash",
				TipoUsuario.ADMIN_CLINICA,
				"S",
				false,
				null,
				null,
				1L
		);
	}

	private AnimalResponse animal() {
		return new AnimalResponse(1L, "Rex", 1L, "Canino", 2L, "SRD", "M", "N", 1L, "Clínica Central", "S");
	}

	private ConsultaResponse consulta() {
		return new ConsultaResponse(
				10L,
				LocalDateTime.of(2026, 8, 30, 10, 0),
				"PRESENCIAL",
				"Consulta anual",
				"Apatia",
				"Sem alterações adicionais",
				new BigDecimal("8.5"),
				"Texto interno",
				StatusConsulta.AG.getCodigo(),
				StatusConsulta.AG.getDescricao(),
				1L,
				"Rex",
				2L,
				"Dra. Vera",
				1L,
				"Clínica Central"
		);
	}

	private PrescricaoResponse prescricao() {
		return new PrescricaoResponse(
				20L,
				"Meloxicam",
				"1 comprimido",
				"A cada 24h",
				"ORAL",
				LocalDate.of(2026, 8, 30),
				LocalDate.of(2026, 9, 4),
				"Administrar após alimentação",
				10L
		);
	}

	private AdesaoPrescricaoResponse adesao() {
		return new AdesaoPrescricaoResponse(
				30L,
				20L,
				3L,
				"Rui Tutor",
				1L,
				"Rex",
				LocalDateTime.of(2026, 8, 30, 12, 0),
				"S",
				"Dose administrada"
		);
	}

	private EspecieResponse especie() {
		return new EspecieResponse(1L, "Canino", "S");
	}

	private RacaResponse raca() {
		return new RacaResponse(2L, "SRD", null, 1L, "Canino", "S");
	}

}
