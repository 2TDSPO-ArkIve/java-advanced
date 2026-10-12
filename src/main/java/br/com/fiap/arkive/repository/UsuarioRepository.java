package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.TipoUsuario;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Profile("!local-nodb")
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByLogin(String login);

	Optional<Usuario> findByLoginIgnoreCase(String login);

	@Query("""
			select u from Usuario u
			join u.veterinario v
			where u.tipo = br.com.fiap.arkive.entity.TipoUsuario.VETERINARIO
			and lower(v.crmv) = lower(:crmv)
			""")
	Optional<Usuario> findVeterinarioByCrmvIgnoreCase(@Param("crmv") String crmv);

	@Query("""
			select distinct u
			from AnimalResponsavel ar, Usuario u
			join fetch u.responsavel r
			where ar.responsavel = r
			and ar.animal.id = :animalId
			and ar.ativo = 'S'
			and ar.id.dataInicio <= :dataAtual
			and (ar.dataFim is null or ar.dataFim >= :dataAtual)
			and u.tipo = br.com.fiap.arkive.entity.TipoUsuario.RESPONSAVEL
			and u.ativo = 'S'
			""")
	List<Usuario> findResponsaveisAtivosVigentesDoAnimal(
			@Param("animalId") Long animalId,
			@Param("dataAtual") LocalDate dataAtual
	);

	Page<Usuario> findByTipoIn(List<TipoUsuario> tipos, Pageable pageable);

	long countByAtivo(String ativo);

	boolean existsByLogin(String login);

	boolean existsByLoginIgnoreCase(String login);

	boolean existsByLoginAndIdNot(String login, Long id);

	boolean existsByVeterinarioId(Long veterinarioId);

	boolean existsByVeterinarioIdAndIdNot(Long veterinarioId, Long id);

	boolean existsByResponsavelId(Long responsavelId);

	boolean existsByResponsavelIdAndIdNot(Long responsavelId, Long id);

	long countByTipoAndAtivo(TipoUsuario tipo, String ativo);

	@Query("""
			select u.tipo, count(u)
			from Usuario u
			group by u.tipo
			order by u.tipo
			""")
	List<Object[]> contarPorPerfil();

}
