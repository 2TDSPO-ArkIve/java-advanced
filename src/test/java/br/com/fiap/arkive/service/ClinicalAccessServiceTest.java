package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClinicalAccessServiceTest {

	private AnimalResponsavelRepository animalResponsavelRepository;
	private ConsultaRepository consultaRepository;
	private ClinicalAccessService clinicalAccessService;

	@BeforeEach
	void setUp() {
		animalResponsavelRepository = mock(AnimalResponsavelRepository.class);
		consultaRepository = mock(ConsultaRepository.class);
		clinicalAccessService = new ClinicalAccessService(animalResponsavelRepository, consultaRepository);
	}

	@Test
	void veterinarioPodeEscreverNaPropriaConsulta() {
		assertDoesNotThrow(() -> clinicalAccessService.exigirEscritaClinicaVeterinario(veterinario(10L), consulta(10L, 30L)));
	}

	@Test
	void veterinarioNaoEscreveEmConsultaDeOutroVeterinario() {
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaClinicaVeterinario(veterinario(10L), consulta(22L, 30L)));
	}

	@Test
	void adminClinicaSysadminEResponsavelNaoExecutamEscritaClinicaDeVeterinario() {
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaClinicaVeterinario(adminClinica(30L), consulta(10L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaClinicaVeterinario(sysadmin(), consulta(10L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaClinicaVeterinario(responsavel(40L), consulta(10L, 30L)));
	}

	@Test
	void consultaPodeSerLidaPorVeterinarioResponsavelResponsavelVinculadoAdminDaClinicaESysadmin() {
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(40L), any(LocalDate.class))).thenReturn(true);

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraConsulta(veterinario(10L), consulta(10L, 30L)));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraConsulta(responsavel(40L), consulta(10L, 30L)));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraConsulta(adminClinica(30L), consulta(10L, 30L)));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraConsulta(sysadmin(), consulta(10L, 30L)));
	}

	@Test
	void consultaNaoPodeSerLidaForaDoEscopoDoUsuario() {
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(41L), any(LocalDate.class))).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraConsulta(veterinario(22L), consulta(10L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraConsulta(responsavel(41L), consulta(10L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraConsulta(adminClinica(31L), consulta(10L, 30L)));
	}

	@Test
	void responsavelLeAnimalSomenteComVinculoAtivoVigente() {
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(40L), any(LocalDate.class))).thenReturn(true);

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAnimal(responsavel(40L), animal(50L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAnimal(responsavel(41L), animal(50L, 30L)));
	}

	@Test
	void veterinarioLeAnimalComConsultaPropria() {
		when(consultaRepository.existsConsultaDoVeterinarioParaAnimal(50L, 10L)).thenReturn(true);

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAnimal(veterinario(10L), animal(50L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAnimal(veterinario(22L), animal(50L, 30L)));
	}

	@Test
	void diagnosticoEPrescricaoUsamConsultaParaEscritaVeterinaria() {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setConsulta(consulta(10L, 30L));
		Prescricao prescricao = new Prescricao();
		prescricao.setConsulta(consulta(10L, 30L));

		assertDoesNotThrow(() -> clinicalAccessService.exigirEscritaDiagnosticoVeterinario(veterinario(10L), diagnostico));
		assertDoesNotThrow(() -> clinicalAccessService.exigirEscritaPrescricaoVeterinario(veterinario(10L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaPrescricaoVeterinario(responsavel(40L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaPrescricaoVeterinario(adminClinica(30L), prescricao));
	}

	private Consulta consulta(Long veterinarioId, Long clinicaId) {
		Consulta consulta = new Consulta();
		consulta.setId(1L);
		consulta.setAnimal(animal(50L, clinicaId));
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		consulta.setVeterinario(veterinario);
		if (clinicaId != null) {
			Clinica clinica = new Clinica();
			clinica.setId(clinicaId);
			consulta.setClinica(clinica);
		}
		return consulta;
	}

	private Animal animal(Long animalId, Long clinicaId) {
		Animal animal = new Animal();
		animal.setId(animalId);
		if (clinicaId != null) {
			Clinica clinica = new Clinica();
			clinica.setId(clinicaId);
			animal.setClinica(clinica);
		}
		return animal;
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}

	private UsuarioPrincipal responsavel(Long responsavelId) {
		return new UsuarioPrincipal(2L, "Tutor", "tutor@arkive.com", "$2a$10$hash", TipoUsuario.RESPONSAVEL, "S", false, responsavelId, null, null);
	}

	private UsuarioPrincipal adminClinica(Long clinicaId) {
		return new UsuarioPrincipal(3L, "Admin", "admin@arkive.com", "$2a$10$hash", TipoUsuario.ADMIN_CLINICA, "S", false, null, null, clinicaId);
	}

	private UsuarioPrincipal sysadmin() {
		return new UsuarioPrincipal(4L, "Sys", "sys@arkive.com", "$2a$10$hash", TipoUsuario.SYSADMIN, "S", false, null, null, null);
	}

}
