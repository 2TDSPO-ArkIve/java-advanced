package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistroVeterinarioRequest;
import br.com.fiap.arkive.dto.response.AuthMeResponse;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({
		VeterinarioService.class,
		UsuarioService.class,
		AccountProvisioningService.class,
		AuthService.class,
		VeterinarioRegistrationProfileIntegrationTest.PasswordConfig.class
})
class VeterinarioRegistrationProfileIntegrationTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private VeterinarioService veterinarioService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private ClinicaService clinicaService;

	@MockitoBean
	private PasswordLifecycleService passwordLifecycleService;

	@MockitoBean
	private ResponsavelRegistrationService responsavelRegistrationService;

	@Test
	void cadastroPublicoProduzIdentidadeQueResolveAuthMeEPerfilProprio() {
		VeterinarioResponse cadastro = authService.registrarVeterinario(
				new RegistroVeterinarioRequest("Dra Vera", "CRMV123", "vera@arkive.com"));
		Usuario usuario = usuarioRepository.findByLoginIgnoreCase("vera@arkive.com").orElseThrow();
		UsuarioPrincipal principal = UsuarioPrincipal.fromEntity(usuario);

		AuthMeResponse me = authService.me(principal);
		VeterinarioResponse perfil = veterinarioService.buscarPerfilAutenticado(principal);

		assertEquals(TipoUsuario.VETERINARIO, usuario.getTipo());
		assertEquals(cadastro.id(), usuario.getVeterinario().getId());
		assertEquals(cadastro.id(), me.veterinarioId());
		assertEquals("CRMV123", me.crmv());
		assertEquals("vera@arkive.com", me.email());
		assertNull(me.clinicaId());
		assertTrue(me.trocaSenhaObrigatoria());
		assertTrue(passwordEncoder.matches("vera@arkive.com", usuario.getSenhaHash()));

		assertEquals(cadastro.id(), perfil.id());
		assertEquals("Dra Vera", perfil.nome());
		assertEquals("CRMV123", perfil.crmv());
		assertEquals("vera@arkive.com", perfil.email());
		assertNull(perfil.especialidade());
		assertNull(perfil.clinicaId());
		assertFalse(perfil.id().equals(999L));
	}

	@TestConfiguration
	static class PasswordConfig {
		@Bean
		PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}
	}
}
