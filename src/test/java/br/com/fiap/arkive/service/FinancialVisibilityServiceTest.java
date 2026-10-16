package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialVisibilityServiceTest {

	private final FinancialVisibilityService service = new FinancialVisibilityService();

	@Test
	void veterinarioVeSomenteCustosDeConsultaPropria() {
		Consulta propria = consulta(10L, 30L);
		Consulta deOutroVeterinario = consulta(11L, 30L);

		assertTrue(service.podeVerCustosConsulta(principal(TipoUsuario.VETERINARIO, null, 10L, null), propria));
		assertFalse(service.podeVerCustosConsulta(
				principal(TipoUsuario.VETERINARIO, null, 10L, null), deOutroVeterinario));
		assertFalse(service.podeVerCustosConsulta(
				principal(TipoUsuario.VETERINARIO, null, null, null), propria));
	}

	@Test
	void responsavelAutorizadoESysadminMantemVisibilidadeEAdminFicaRestritoAClinica() {
		Consulta consulta = consulta(10L, 30L);

		assertTrue(service.podeVerCustosConsulta(principal(TipoUsuario.RESPONSAVEL, 40L, null, null), consulta));
		assertTrue(service.podeVerCustosConsulta(principal(TipoUsuario.SYSADMIN, null, null, null), consulta));
		assertTrue(service.podeVerCustosConsulta(principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L), consulta));
		assertFalse(service.podeVerCustosConsulta(principal(TipoUsuario.ADMIN_CLINICA, null, null, 31L), consulta));
	}

	@Test
	void contextoInvalidoFalhaFechado() {
		assertFalse(service.podeVerCustosConsulta(null, consulta(10L, 30L)));
		assertFalse(service.podeVerCustosConsulta(principal(TipoUsuario.VETERINARIO, null, 10L, null), null));
	}

	private Consulta consulta(Long veterinarioId, Long clinicaId) {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		Clinica clinica = new Clinica();
		clinica.setId(clinicaId);
		Consulta consulta = new Consulta();
		consulta.setVeterinario(veterinario);
		consulta.setClinica(clinica);
		return consulta;
	}

	private UsuarioPrincipal principal(TipoUsuario tipo, Long responsavelId, Long veterinarioId, Long clinicaId) {
		return new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "hash", tipo,
				"S", false, responsavelId, veterinarioId, clinicaId);
	}
}
