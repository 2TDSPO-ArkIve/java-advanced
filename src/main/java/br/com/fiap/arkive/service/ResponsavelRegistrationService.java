package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistroResponsavelRequest;
import br.com.fiap.arkive.dto.response.RegistroResponsavelResponse;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.PasswordPolicy;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Profile("!local-nodb")
public class ResponsavelRegistrationService {

	private final ResponsavelRepository responsavelRepository;
	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicy passwordPolicy;

	public ResponsavelRegistrationService(ResponsavelRepository responsavelRepository,
			UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy) {
		this.responsavelRepository = responsavelRepository;
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
	}

	@Transactional
	public RegistroResponsavelResponse registrar(RegistroResponsavelRequest request) {
		String nome = request.nome().trim();
		String documento = request.documento().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String telefone = vazioParaNulo(request.telefone());

		passwordPolicy.validar(request.senha());
		passwordPolicy.validarConfirmacao(request.senha(), request.confirmacaoSenha());
		validarDisponibilidade(email, documento);

		try {
			Responsavel responsavel = new Responsavel();
			responsavel.setNome(nome);
			responsavel.setDocumento(documento);
			responsavel.setEmail(email);
			responsavel.setTelefone(telefone);
			responsavel.setTipo("TUTOR");
			responsavel.setDataCadastro(LocalDate.now());
			responsavel.setNotificacao("S");
			responsavel.setAtivo("S");
			Responsavel responsavelSalvo = responsavelRepository.saveAndFlush(responsavel);

			Usuario usuario = new Usuario();
			usuario.setNome(nome);
			usuario.setTipo(TipoUsuario.RESPONSAVEL);
			usuario.setResponsavel(responsavelSalvo);
			usuario.setVeterinario(null);
			usuario.setClinica(null);
			usuario.setLogin(email);
			usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
			usuario.setDataCadastro(LocalDateTime.now());
			usuario.setAtivo("S");
			usuario.setTrocaSenha("N");
			usuario.setDataUltimaTrocaSenha(LocalDateTime.now());
			Usuario usuarioSalvo = usuarioRepository.saveAndFlush(usuario);

			return new RegistroResponsavelResponse(
					usuarioSalvo.getId(), responsavelSalvo.getId(), usuarioSalvo.getNome(),
					usuarioSalvo.getLogin(), usuarioSalvo.getTipo(), usuarioSalvo.getAtivo(), false
			);
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Ja existe uma conta com o e-mail ou documento informado.", HttpStatus.CONFLICT);
		}
	}

	private void validarDisponibilidade(String email, String documento) {
		if (usuarioRepository.existsByLoginIgnoreCase(email)) {
			throw new BusinessException("E-mail/login ja cadastrado.", HttpStatus.CONFLICT);
		}
		if (responsavelRepository.existsByDocumento(documento)) {
			throw new BusinessException("Documento de responsavel ja cadastrado.", HttpStatus.CONFLICT);
		}
	}

	private String vazioParaNulo(String valor) {
		if (valor == null || valor.isBlank()) return null;
		return valor.trim();
	}
}
