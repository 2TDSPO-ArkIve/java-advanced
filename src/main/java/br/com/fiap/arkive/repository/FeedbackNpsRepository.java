package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.FeedbackNps;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface FeedbackNpsRepository extends JpaRepository<FeedbackNps, Long> {

	@Query("""
			select f from FeedbackNps f
			where (:responsavelId is null or f.responsavel.id = :responsavelId)
			and (:animalId is null or f.animal.id = :animalId)
			and (:clinicaId is null or f.clinica.id = :clinicaId)
			and (:consultaId is null or f.consulta.id = :consultaId)
			and (:nota is null or f.nota = :nota)
			""")
	Page<FeedbackNps> buscar(
			@Param("responsavelId") Long responsavelId,
			@Param("animalId") Long animalId,
			@Param("clinicaId") Long clinicaId,
			@Param("consultaId") Long consultaId,
			@Param("nota") Integer nota,
			Pageable pageable
	);

}
