package br.com.fiap.arkive.security;

import br.com.fiap.arkive.entity.TipoUsuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioPrincipalTest {

	@Test
	void mapeiaSysadminParaRoleSysadmin() {
		assertEquals("ROLE_SYSADMIN", principal(TipoUsuario.SYSADMIN, "S").getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void mapeiaAdminClinicaParaRoleAdminClinica() {
		assertEquals("ROLE_ADMIN_CLINICA", principal(TipoUsuario.ADMIN_CLINICA, "S").getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void mapeiaVeterinarioParaRoleVeterinario() {
		assertEquals("ROLE_VETERINARIO", principal(TipoUsuario.VETERINARIO, "S").getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void mapeiaResponsavelParaRoleResponsavel() {
		assertEquals("ROLE_RESPONSAVEL", principal(TipoUsuario.RESPONSAVEL, "S").getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void ativoSHabilitaUsuario() {
		assertTrue(principal(TipoUsuario.SYSADMIN, "S").isEnabled());
	}

	@Test
	void ativoNDesabilitaUsuario() {
		assertFalse(principal(TipoUsuario.SYSADMIN, "N").isEnabled());
	}

	private UsuarioPrincipal principal(TipoUsuario tipo, String ativo) {
		return new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "$2a$10$hash", tipo, ativo);
	}

}
