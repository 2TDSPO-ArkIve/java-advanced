package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistrarDispositivoRequest;
import br.com.fiap.arkive.dto.response.UsuarioDispositivoResponse;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.UsuarioDispositivo;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.UsuarioDispositivoRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Profile("!local-nodb")
public class UsuarioDispositivoService {

	private final UsuarioDispositivoRepository dispositivoRepository;
	private final UsuarioRepository usuarioRepository;

	public UsuarioDispositivoService(UsuarioDispositivoRepository dispositivoRepository,
			UsuarioRepository usuarioRepository) {
		this.dispositivoRepository = dispositivoRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional
	public UsuarioDispositivoResponse registrar(RegistrarDispositivoRequest request, UsuarioPrincipal principal) {
		Long usuarioId = exigirUsuario(principal);
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.filter(entity -> "S".equals(entity.getAtivo()))
				.orElseThrow(() -> new AccessDeniedException("Usuario autenticado invalido."));
		Optional<UsuarioDispositivo> porInstalacao = dispositivoRepository
				.findByUsuarioIdAndInstalacaoId(usuarioId, request.installationId());
		Optional<UsuarioDispositivo> porToken = dispositivoRepository.findByExpoPushToken(request.expoPushToken());

		UsuarioDispositivo dispositivo;
		if (porInstalacao.isPresent()) {
			dispositivo = porInstalacao.get();
			if (porToken.isPresent() && !porToken.get().getId().equals(dispositivo.getId())) {
				throw new BusinessException("Expo push token ja registrado para outra instalacao.", HttpStatus.CONFLICT);
			}
		} else if (porToken.isPresent()) {
			dispositivo = porToken.get();
			if ("S".equals(dispositivo.getAtivo()) && !dispositivo.getUsuario().getId().equals(usuarioId)) {
				throw new BusinessException("Expo push token ja registrado para outra conta.", HttpStatus.CONFLICT);
			}
			dispositivo.setUsuario(usuario);
			dispositivo.setInstalacaoId(request.installationId());
		} else {
			dispositivo = new UsuarioDispositivo();
			dispositivo.setUsuario(usuario);
			dispositivo.setInstalacaoId(request.installationId());
			dispositivo.setDataCriacao(LocalDateTime.now());
		}

		dispositivo.setExpoPushToken(request.expoPushToken());
		dispositivo.setPlataforma(request.platform());
		dispositivo.setDataUltimoContato(LocalDateTime.now());
		dispositivo.setAtivo("S");
		dispositivo.setDataRevogacao(null);
		return UsuarioDispositivoResponse.fromEntity(dispositivoRepository.save(dispositivo));
	}

	@Transactional
	public void desativar(Long id, UsuarioPrincipal principal) {
		Long usuarioId = exigirUsuario(principal);
		UsuarioDispositivo dispositivo = dispositivoRepository.findByIdAndUsuarioId(id, usuarioId)
				.orElseThrow(() -> new ResourceNotFoundException("Dispositivo nao encontrado."));
		if (!"N".equals(dispositivo.getAtivo())) {
			dispositivo.setAtivo("N");
			dispositivo.setDataRevogacao(LocalDateTime.now());
			dispositivo.setDataUltimoContato(LocalDateTime.now());
			dispositivoRepository.save(dispositivo);
		}
	}

	private Long exigirUsuario(UsuarioPrincipal principal) {
		if (principal == null || principal.getUsuarioId() == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
		return principal.getUsuarioId();
	}
}
