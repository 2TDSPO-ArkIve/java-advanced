package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Alerta;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

	@Query("""
			select a from Alerta a
			where (:animalId is null or a.animal.id = :animalId)
			and (:responsavelId is null or a.responsavel.id = :responsavelId)
			and (:clinicaId is null or a.clinica.id = :clinicaId)
			and (:eventoPreventivoId is null or a.eventoPreventivo.id = :eventoPreventivoId)
			and (:tipo is null or a.tipo = :tipo)
			and (:status is null or a.status = :status)
			and (:canal is null or a.canal = :canal)
			""")
	Page<Alerta> buscar(
			@Param("animalId") Long animalId,
			@Param("responsavelId") Long responsavelId,
			@Param("clinicaId") Long clinicaId,
			@Param("eventoPreventivoId") Long eventoPreventivoId,
			@Param("tipo") String tipo,
			@Param("status") String status,
			@Param("canal") String canal,
			Pageable pageable
	);

}
