package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Prescricao;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface PrescricaoRepository extends JpaRepository<Prescricao, Long> {

	@Query("""
			select p from Prescricao p
			where (:consultaId is null or p.consulta.id = :consultaId)
			and (:medicamento is null or lower(p.medicamento) like lower(concat('%', :medicamento, '%')))
			""")
	Page<Prescricao> buscar(
			@Param("consultaId") Long consultaId,
			@Param("medicamento") String medicamento,
			Pageable pageable
	);

}
