package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.EventoJornada;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface EventoJornadaRepository extends JpaRepository<EventoJornada, Long> {

	@Query("""
			select e from EventoJornada e
			where (:tipoEvento is null or e.tipoEvento = :tipoEvento)
			and (:origem is null or e.origem = :origem)
			and (:ator is null or e.ator = :ator)
			and (:responsavelId is null or e.responsavel.id = :responsavelId)
			and (:veterinarioId is null or e.veterinario.id = :veterinarioId)
			and (:animalId is null or e.animal.id = :animalId)
			and (:clinicaId is null or e.clinica.id = :clinicaId)
			and (:canal is null or e.canal = :canal)
			""")
	Page<EventoJornada> buscar(
			@Param("tipoEvento") String tipoEvento,
			@Param("origem") String origem,
			@Param("ator") String ator,
			@Param("responsavelId") Long responsavelId,
			@Param("veterinarioId") Long veterinarioId,
			@Param("animalId") Long animalId,
			@Param("clinicaId") Long clinicaId,
			@Param("canal") String canal,
			Pageable pageable
	);

	Page<EventoJornada> findByAnimalId(Long animalId, Pageable pageable);

}
