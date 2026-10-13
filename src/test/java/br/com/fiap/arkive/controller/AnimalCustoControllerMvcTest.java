package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.PatientAccessRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AnimalCustoService;
import br.com.fiap.arkive.service.ClinicalAccessService;
import br.com.fiap.arkive.service.VeterinarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalCustoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, AnimalCustoService.class, ClinicalAccessService.class})
class AnimalCustoControllerMvcTest {

	@Autowired
	MockMvc mvc;

	@MockitoBean ArkiveUserDetailsService usuarios;
	@MockitoBean AnimalRepository animais;
	@MockitoBean ConsultaRepository consultas;
	@MockitoBean PrescricaoRepository prescricoes;
	@MockitoBean AnimalResponsavelRepository responsaveis;
	@MockitoBean PatientAccessRepository acessosExplicitos;
	@MockitoBean VeterinarioService veterinarios;

	@BeforeEach
	void setUp() {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(10L);
		Animal animal = new Animal();
		animal.setId(50L);
		animal.setNome("Epona");
		animal.setAtivo("S");
		animal.setVeterinarioCadastro(veterinario);
		animal.assignArkivePatientId("ARK-PET-EPONA");
		Consulta consulta = new Consulta();
		consulta.setId(42L);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setDataHora(LocalDateTime.parse("2026-10-12T14:30:00"));
		consulta.setStatus("FI");
		when(animais.findById(50L)).thenReturn(Optional.of(animal));
		when(consultas.buscarCustosFinalizadosPorAnimal(50L)).thenReturn(List.of(consulta));
		when(prescricoes.somarCustosMedicamentosFinalizadosPorAnimal(50L)).thenReturn(List.of());
	}

	@Test
	void veterinarioAutorizadoVisualizaResumo() throws Exception {
		mvc.perform(get("/api/animais/50/custos").with(user(principalVeterinario(10L))))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.animalId").value(50))
				.andExpect(jsonPath("$.arkivePatientId").value("ARK-PET-EPONA"))
				.andExpect(jsonPath("$.totalGeral").value(0.0))
				.andExpect(jsonPath("$.consultas[0].consultaId").value(42));
	}

	@Test
	void responsavelComVinculoVisualizaResumo() throws Exception {
		when(responsaveis.existsVinculoAtivoVigente(eq(50L), eq(40L), any(LocalDate.class))).thenReturn(true);

		mvc.perform(get("/api/animais/50/custos").with(user(principalResponsavel())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.animalNome").value("Epona"));
	}

	@Test
	void veterinarioSemAcessoEhRejeitado() throws Exception {
		mvc.perform(get("/api/animais/50/custos").with(user(principalVeterinario(22L))))
				.andExpect(status().isForbidden());

		verify(consultas, never()).buscarCustosFinalizadosPorAnimal(50L);
	}

	@Test
	void responsavelSemVinculoEhRejeitadoMesmoConhecendoArkivePatientId() throws Exception {
		mvc.perform(get("/api/animais/50/custos").with(user(principalResponsavel())))
				.andExpect(status().isForbidden());

		verify(consultas, never()).buscarCustosFinalizadosPorAnimal(50L);
	}

	@Test
	void acessoNaoAutenticadoEhRejeitado() throws Exception {
		mvc.perform(get("/api/animais/50/custos"))
				.andExpect(status().isUnauthorized());

		verify(animais, never()).findById(50L);
	}

	private UsuarioPrincipal principalVeterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "hash", TipoUsuario.VETERINARIO,
				"S", false, null, veterinarioId, null);
	}

	private UsuarioPrincipal principalResponsavel() {
		return new UsuarioPrincipal(2L, "Tutor", "tutor@arkive.com", "hash", TipoUsuario.RESPONSAVEL,
				"S", false, 40L, null, null);
	}
}
