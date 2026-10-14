package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.AnimalResponsavelId;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.repository.NotificacaoRepository;
import br.com.fiap.arkive.service.notification.NotificacaoPushListener;
import br.com.fiap.arkive.service.notification.NotificacaoPushService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({NotificacaoRecipientService.class, NotificacaoService.class, NotificacaoPushListener.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificacaoTransactionIntegrationTest {

	@Autowired EntityManager em;
	@Autowired NotificacaoService notificacaoService;
	@Autowired NotificacaoRepository notificacaoRepository;
	@Autowired PlatformTransactionManager transactionManager;
	@MockitoBean NotificacaoPushService pushService;

	@BeforeEach
	void limparNotificacoes() {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> notificacaoRepository.deleteAll());
		reset(pushService);
	}

	@Test
	void publicaPushSomenteDepoisDoCommitEIdempotenciaEvitaDuplicata() {
		Consulta consulta = prepararConsultaComResponsavelAtual("S");
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		transaction.executeWithoutResult(status -> {
			notificacaoService.criarParaConsultaFinalizada(consulta);
			notificacaoService.criarParaConsultaFinalizada(consulta);
			verify(pushService, never()).entregar(anyList());
		});

		assertEquals(1, notificacaoRepository.count());
		verify(pushService).entregar(List.of(notificacaoRepository.findAll().get(0).getId()));
	}

	@Test
	void rollbackRemoveNotificacaoENaoDisparaPush() {
		Consulta consulta = prepararConsultaComResponsavelAtual("S");
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		transaction.executeWithoutResult(status -> {
			notificacaoService.criarParaConsultaFinalizada(consulta);
			status.setRollbackOnly();
		});

		assertEquals(0, notificacaoRepository.count());
		verify(pushService, never()).entregar(anyList());
	}

	private Consulta prepararConsultaComResponsavelAtual(String preferencia) {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		return transaction.execute(status -> {
			Especie especie = new Especie();
			especie.setNome("Canino-" + System.nanoTime());
			em.persist(especie);
			Animal animal = new Animal();
			animal.setNome("Rex");
			animal.setEspecie(especie);
			em.persist(animal);
			Responsavel responsavel = new Responsavel();
			responsavel.setNome("Responsável");
			responsavel.setDocumento("doc-" + System.nanoTime());
			responsavel.setTipo("CUIDADOR");
			responsavel.setNotificacao(preferencia);
			em.persist(responsavel);
			Usuario usuario = new Usuario();
			usuario.setNome("Responsável");
			usuario.setTipo(TipoUsuario.RESPONSAVEL);
			usuario.setResponsavel(responsavel);
			usuario.setLogin("responsavel-" + System.nanoTime() + "@example.test");
			usuario.setSenhaHash("hash");
			em.persist(usuario);
			AnimalResponsavelId vinculoId = new AnimalResponsavelId();
			vinculoId.setAnimalId(animal.getId());
			vinculoId.setResponsavelId(responsavel.getId());
			vinculoId.setDataInicio(LocalDate.now().minusDays(1));
			AnimalResponsavel vinculo = new AnimalResponsavel();
			vinculo.setId(vinculoId);
			vinculo.setAnimal(animal);
			vinculo.setResponsavel(responsavel);
			vinculo.setTipoVinculo("CUIDADOR");
			em.persist(vinculo);
			Consulta consulta = new Consulta();
			consulta.setAnimal(animal);
			consulta.setDataHora(LocalDateTime.now());
			consulta.setModalidade("PRESENCIAL");
			consulta.setMotivo("Check-up");
			// The notification trigger needs only the persisted consultation identity and animal.
			em.flush();
			consulta.setId(700L + animal.getId());
			return consulta;
		});
	}
}
