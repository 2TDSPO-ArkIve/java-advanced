package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.TipoUsuario;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Profile("!local-nodb")
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByLogin(String login);

	boolean existsByLogin(String login);

	long countByTipoAndAtivo(TipoUsuario tipo, String ativo);

}
