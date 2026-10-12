package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistroResponsavelRequest;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@Import({ResponsavelRegistrationService.class, PasswordPolicy.class})
class ResponsavelRegistrationRollbackTest {

	@Autowired ResponsavelRegistrationService registrationService;
	@Autowired ResponsavelRepository responsavelRepository;
	@Autowired UsuarioRepository usuarioRepository;
	@MockitoBean PasswordEncoder passwordEncoder;

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void falhaNaPersistenciaDoUsuarioNaoDeixaResponsavelOrfao() {
		when(passwordEncoder.encode("SenhaSegura1")).thenReturn(null);
		RegistroResponsavelRequest request = new RegistroResponsavelRequest(
				"Maria Tutor", "12345678900", "maria@arkive.com", null,
				"SenhaSegura1", "SenhaSegura1");

		assertThrows(BusinessException.class, () -> registrationService.registrar(request));

		assertEquals(0, responsavelRepository.count());
		assertEquals(0, usuarioRepository.count());
	}
}
