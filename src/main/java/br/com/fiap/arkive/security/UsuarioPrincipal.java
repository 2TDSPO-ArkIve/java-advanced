package br.com.fiap.arkive.security;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UsuarioPrincipal implements UserDetails {

	private final Long usuarioId;
	private final String nome;
	private final String login;
	private final String senhaHash;
	private final TipoUsuario tipoUsuario;
	private final String ativo;

	public UsuarioPrincipal(Long usuarioId, String nome, String login, String senhaHash, TipoUsuario tipoUsuario, String ativo) {
		this.usuarioId = usuarioId;
		this.nome = nome;
		this.login = login;
		this.senhaHash = senhaHash;
		this.tipoUsuario = tipoUsuario;
		this.ativo = ativo;
	}

	public static UsuarioPrincipal fromEntity(Usuario usuario) {
		return new UsuarioPrincipal(
				usuario.getId(),
				usuario.getNome(),
				usuario.getLogin(),
				usuario.getSenhaHash(),
				usuario.getTipo(),
				usuario.getAtivo()
		);
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public String getNome() {
		return nome;
	}

	public TipoUsuario getTipoUsuario() {
		return tipoUsuario;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + tipoUsuario.name()));
	}

	@Override
	public String getPassword() {
		return senhaHash;
	}

	@Override
	public String getUsername() {
		return login;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return "S".equals(ativo);
	}

}
