package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.TipoUsuario;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Profile("!local-nodb")
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByLogin(String login);

	Page<Usuario> findByTipoIn(List<TipoUsuario> tipos, Pageable pageable);

	long countByAtivo(String ativo);

	boolean existsByLogin(String login);

	long countByTipoAndAtivo(TipoUsuario tipo, String ativo);

	@Query("""
			select u.tipo, count(u)
			from Usuario u
			group by u.tipo
			order by u.tipo
			""")
	List<Object[]> contarPorPerfil();

}
