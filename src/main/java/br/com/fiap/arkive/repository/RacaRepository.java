package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Raca;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface RacaRepository extends JpaRepository<Raca, Long> {

	@Query("""
			select r from Raca r
			where (:nome is null or lower(r.nome) like lower(concat('%', :nome, '%')))
			and (:especieId is null or r.especie.id = :especieId)
			""")
	Page<Raca> buscar(@Param("nome") String nome, @Param("especieId") Long especieId, Pageable pageable);

}
