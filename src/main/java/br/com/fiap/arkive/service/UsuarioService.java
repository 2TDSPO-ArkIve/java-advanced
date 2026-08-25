package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.dto.response.UsuarioResponse;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final ResponsavelRepository responsavelRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final ClinicaRepository clinicaRepository;

	public UsuarioService(
			UsuarioRepository usuarioRepository,
			ResponsavelRepository responsavelRepository,
			VeterinarioRepository veterinarioRepository,
			ClinicaRepository clinicaRepository
	) {
		this.usuarioRepository = usuarioRepository;
		this.responsavelRepository = responsavelRepository;
		this.veterinarioRepository = veterinarioRepository;
		this.clinicaRepository = clinicaRepository;
	}

	@Transactional(readOnly = true)
	public Page<UsuarioResponse> listar(Pageable pageable) {
		return usuarioRepository.findAll(pageable).map(UsuarioResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorId(Long id) {
		return UsuarioResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorLogin(String login) {
		return UsuarioResponse.fromEntity(usuarioRepository.findByLogin(login)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado.")));
	}

	@Transactional(readOnly = true)
	public void validarCriacao(UsuarioRequest request) {
		validarCamposObrigatorios(request);
		validarSNQuandoInformado(request.ativo(), "Ativo");
		validarLoginDisponivel(request.login());
		validarAssociacoes(request);
	}

	private Usuario buscarEntidade(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
	}

	private void validarCamposObrigatorios(UsuarioRequest request) {
		if (request.tipo() == null) {
			throw new BusinessException("Tipo de usuario e obrigatorio.");
		}
		if (request.nome() == null || request.nome().isBlank()) {
			throw new BusinessException("Nome do usuario e obrigatorio.");
		}
		if (request.login() == null || request.login().isBlank()) {
			throw new BusinessException("Login do usuario e obrigatorio.");
		}
		if (request.senha() == null || request.senha().isBlank()) {
			throw new BusinessException("Senha do usuario e obrigatoria.");
		}
	}

	private void validarLoginDisponivel(String login) {
		if (usuarioRepository.existsByLogin(login)) {
			throw new BusinessException("Login de usuario ja cadastrado.");
		}
	}

	private void validarAssociacoes(UsuarioRequest request) {
		TipoUsuario tipo = request.tipo();
		switch (tipo) {
			case SYSADMIN -> validarSysadmin(request);
			case ADMIN_CLINICA -> validarAdminClinica(request);
			case VETERINARIO -> validarVeterinario(request);
			case RESPONSAVEL -> validarResponsavel(request);
		}
	}

	private void validarSysadmin(UsuarioRequest request) {
		if (request.responsavelId() != null || request.veterinarioId() != null || request.clinicaId() != null) {
			throw new BusinessException("Usuario SYSADMIN nao deve ter responsavel, veterinario ou clinica associados.");
		}
	}

	private void validarAdminClinica(UsuarioRequest request) {
		if (request.clinicaId() == null) {
			throw new BusinessException("Usuario ADMIN_CLINICA deve estar associado a uma clinica.");
		}
		if (request.responsavelId() != null || request.veterinarioId() != null) {
			throw new BusinessException("Usuario ADMIN_CLINICA nao deve ter responsavel ou veterinario associados.");
		}
		validarClinicaAtiva(request.clinicaId());
	}

	private void validarVeterinario(UsuarioRequest request) {
		if (request.veterinarioId() == null) {
			throw new BusinessException("Usuario VETERINARIO deve estar associado a um veterinario.");
		}
		if (request.responsavelId() != null || request.clinicaId() != null) {
			throw new BusinessException("Usuario VETERINARIO nao deve ter responsavel ou clinica associados.");
		}
		validarVeterinarioAtivo(request.veterinarioId());
	}

	private void validarResponsavel(UsuarioRequest request) {
		if (request.responsavelId() == null) {
			throw new BusinessException("Usuario RESPONSAVEL deve estar associado a um responsavel.");
		}
		if (request.veterinarioId() != null || request.clinicaId() != null) {
			throw new BusinessException("Usuario RESPONSAVEL nao deve ter veterinario ou clinica associados.");
		}
		validarResponsavelAtivo(request.responsavelId());
	}

	private void validarResponsavelAtivo(Long id) {
		Responsavel responsavel = responsavelRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
		if (!"S".equals(responsavel.getAtivo())) {
			throw new BusinessException("Responsavel associado ao usuario deve estar ativo.");
		}
	}

	private void validarVeterinarioAtivo(Long id) {
		Veterinario veterinario = veterinarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Veterinario nao encontrado."));
		if (!"S".equals(veterinario.getAtivo())) {
			throw new BusinessException("Veterinario associado ao usuario deve estar ativo.");
		}
	}

	private void validarClinicaAtiva(Long id) {
		Clinica clinica = clinicaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada."));
		if (!"S".equals(clinica.getAtivo())) {
			throw new BusinessException("Clinica associada ao usuario deve estar ativa.");
		}
	}

	private void validarSNQuandoInformado(String valor, String campo) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException(campo + " deve ser S ou N.");
		}
	}

}
