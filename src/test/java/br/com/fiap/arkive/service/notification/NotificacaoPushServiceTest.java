package br.com.fiap.arkive.service.notification;

import br.com.fiap.arkive.config.ExpoPushProperties;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Notificacao;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoNotificacao;
import br.com.fiap.arkive.entity.TipoRecursoNotificacao;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.UsuarioDispositivo;
import br.com.fiap.arkive.repository.NotificacaoRepository;
import br.com.fiap.arkive.repository.UsuarioDispositivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoPushServiceTest {

	private NotificacaoRepository notificacaoRepository;
	private UsuarioDispositivoRepository dispositivoRepository;
	private ExpoPushClient client;
	private ExpoPushProperties properties;
	private NotificacaoPushService service;

	@BeforeEach
	void setUp() {
		notificacaoRepository = mock(NotificacaoRepository.class);
		dispositivoRepository = mock(UsuarioDispositivoRepository.class);
		client = mock(ExpoPushClient.class);
		properties = new ExpoPushProperties();
		properties.setEnabled(true);
		service = new NotificacaoPushService(notificacaoRepository, dispositivoRepository, client, properties);
	}

	@Test
	void desabilitadoNaoConsultaBancoNemTentaPush() {
		properties.setEnabled(false);

		service.entregar(List.of(1L));

		verify(notificacaoRepository, never()).findById(any());
		verify(client, never()).enviar(any());
	}

	@Test
	void preferenciaNaoMantemInboxMasSuprimePush() {
		Notificacao notificacao = notificacao("N");
		when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));

		service.entregar(List.of(1L));

		verify(dispositivoRepository, never()).findByUsuarioIdAndAtivoOrderByIdAsc(any(), any());
		verify(client, never()).enviar(any());
	}

	@Test
	void preferenciaSimEnviaPayloadCompactoParaDispositivoAtivo() {
		Notificacao notificacao = notificacao("S");
		UsuarioDispositivo dispositivo = dispositivo(notificacao.getUsuario());
		when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));
		when(dispositivoRepository.findByUsuarioIdAndAtivoOrderByIdAsc(10L, "S"))
				.thenReturn(List.of(dispositivo));
		when(client.enviar(any())).thenReturn(ExpoPushResult.SUCCESS);

		service.entregar(List.of(1L));

		ArgumentCaptor<ExpoPushMessage> captor = ArgumentCaptor.forClass(ExpoPushMessage.class);
		verify(client).enviar(captor.capture());
		ExpoPushMessage message = captor.getValue();
		assertEquals("ExponentPushToken[secret]", message.to());
		assertEquals("Atualização no ArkIve", message.title());
		assertEquals(1L, message.data().get("notificationId"));
		assertEquals("CONSULTA_FINALIZADA", message.data().get("type"));
		assertEquals(5L, message.data().get("animalId"));
		assertEquals("CONSULTA", message.data().get("resourceType"));
		assertEquals(77L, message.data().get("resourceId"));
		assertEquals(5, message.data().size());
	}

	@ParameterizedTest
	@EnumSource(value = TipoNotificacao.class, names = { "CONSULTA_AGENDADA", "LEMBRETE_CONSULTA" })
	void novosTiposFluemNoMesmoPayloadSemAlterarSchema(TipoNotificacao tipo) {
		Notificacao notificacao = notificacao("S", tipo);
		when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));
		when(dispositivoRepository.findByUsuarioIdAndAtivoOrderByIdAsc(10L, "S"))
				.thenReturn(List.of(dispositivo(notificacao.getUsuario())));
		when(client.enviar(any())).thenReturn(ExpoPushResult.SUCCESS);

		service.entregar(List.of(1L));

		ArgumentCaptor<ExpoPushMessage> captor = ArgumentCaptor.forClass(ExpoPushMessage.class);
		verify(client).enviar(captor.capture());
		assertEquals(tipo.name(), captor.getValue().data().get("type"));
		assertEquals(5, captor.getValue().data().size());
	}

	@Test
	void deviceNotRegisteredDesativaSomenteDispositivoAfetado() {
		Notificacao notificacao = notificacao("S");
		UsuarioDispositivo dispositivo = dispositivo(notificacao.getUsuario());
		when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));
		when(dispositivoRepository.findByUsuarioIdAndAtivoOrderByIdAsc(10L, "S"))
				.thenReturn(List.of(dispositivo));
		when(client.enviar(any())).thenReturn(ExpoPushResult.DEVICE_NOT_REGISTERED);

		service.entregar(List.of(1L));

		assertEquals("N", dispositivo.getAtivo());
		assertNotNull(dispositivo.getDataRevogacao());
		verify(dispositivoRepository).save(dispositivo);
	}

	@Test
	void excecaoInesperadaDeUmPushNaoEscapaNemApagaNotificacao() {
		Notificacao notificacao = notificacao("S");
		when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));
		when(dispositivoRepository.findByUsuarioIdAndAtivoOrderByIdAsc(10L, "S"))
				.thenReturn(List.of(dispositivo(notificacao.getUsuario())));
		doThrow(new IllegalStateException("token secreto nao deve aparecer")).when(client).enviar(any());

		service.entregar(List.of(1L));

		verify(notificacaoRepository, never()).delete(any());
	}

	private Notificacao notificacao(String preferencia) {
		return notificacao(preferencia, TipoNotificacao.CONSULTA_FINALIZADA);
	}

	private Notificacao notificacao(String preferencia, TipoNotificacao tipo) {
		Responsavel responsavel = new Responsavel();
		responsavel.setNotificacao(preferencia);
		Usuario usuario = new Usuario();
		usuario.setId(10L);
		usuario.setTipo(TipoUsuario.RESPONSAVEL);
		usuario.setResponsavel(responsavel);
		Animal animal = new Animal();
		animal.setId(5L);
		Notificacao notificacao = new Notificacao();
		notificacao.setId(1L);
		notificacao.setUsuario(usuario);
		notificacao.setTipo(tipo);
		notificacao.setTitulo("Atualização no ArkIve");
		notificacao.setMensagem("Há uma nova atualização no acompanhamento de Rex.");
		notificacao.setAnimal(animal);
		notificacao.setTipoRecurso(TipoRecursoNotificacao.CONSULTA);
		notificacao.setRecursoId(77L);
		return notificacao;
	}

	private UsuarioDispositivo dispositivo(Usuario usuario) {
		UsuarioDispositivo dispositivo = new UsuarioDispositivo();
		dispositivo.setId(20L);
		dispositivo.setUsuario(usuario);
		dispositivo.setExpoPushToken("ExponentPushToken[secret]");
		dispositivo.setAtivo("S");
		return dispositivo;
	}
}
