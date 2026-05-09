package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.AdesaoPrescricao;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface AdesaoPrescricaoRepository extends JpaRepository<AdesaoPrescricao, Long> {

	@Query("""
			select a from AdesaoPrescricao a
			where (:prescricaoId is null or a.prescricao.id = :prescricaoId)
			and (:animalId is null or a.animal.id = :animalId)
			and (:responsavelId is null or a.responsavel.id = :responsavelId)
			and (:tomou is null or a.tomou = :tomou)
			""")
	Page<AdesaoPrescricao> buscar(
			@Param("prescricaoId") Long prescricaoId,
			@Param("animalId") Long animalId,
			@Param("responsavelId") Long responsavelId,
			@Param("tomou") String tomou,
			Pageable pageable
	);

}
