package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.AvaliacaoBemEstar;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface AvaliacaoBemEstarRepository extends JpaRepository<AvaliacaoBemEstar, Long> {

	@Query("""
			select a from AvaliacaoBemEstar a
			where (:animalId is null or a.animal.id = :animalId)
			and (:responsavelId is null or a.responsavel.id = :responsavelId)
			and (:veterinarioId is null or a.veterinario.id = :veterinarioId)
			and (:consultaId is null or a.consulta.id = :consultaId)
			and (:apetite is null or a.apetite = :apetite)
			and (:atividade is null or a.atividade = :atividade)
			and (:comportamento is null or a.comportamento = :comportamento)
			""")
	Page<AvaliacaoBemEstar> buscar(
			@Param("animalId") Long animalId,
			@Param("responsavelId") Long responsavelId,
			@Param("veterinarioId") Long veterinarioId,
			@Param("consultaId") Long consultaId,
			@Param("apetite") String apetite,
			@Param("atividade") String atividade,
			@Param("comportamento") String comportamento,
			Pageable pageable
	);

}
