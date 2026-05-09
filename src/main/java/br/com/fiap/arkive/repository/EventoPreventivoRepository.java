package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.EventoPreventivo;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface EventoPreventivoRepository extends JpaRepository<EventoPreventivo, Long> {

	@Query("""
			select e from EventoPreventivo e
			where (:animalId is null or e.animal.id = :animalId)
			and (:protocoloId is null or e.protocolo.id = :protocoloId)
			and (:consultaId is null or e.consulta.id = :consultaId)
			and (:status is null or e.status = :status)
			and (:alerta is null or e.alerta = :alerta)
			""")
	Page<EventoPreventivo> buscar(
			@Param("animalId") Long animalId,
			@Param("protocoloId") Long protocoloId,
			@Param("consultaId") Long consultaId,
			@Param("status") String status,
			@Param("alerta") String alerta,
			Pageable pageable
	);

}
