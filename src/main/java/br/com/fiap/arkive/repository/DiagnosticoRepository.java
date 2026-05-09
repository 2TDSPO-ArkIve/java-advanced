package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Diagnostico;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface DiagnosticoRepository extends JpaRepository<Diagnostico, Long> {

	@Query("""
			select d from Diagnostico d
			where (:consultaId is null or d.consulta.id = :consultaId)
			and (:doencaId is null or d.doenca.id = :doencaId)
			and (:severidade is null or d.severidade = :severidade)
			and (:confirmado is null or d.confirmado = :confirmado)
			""")
	Page<Diagnostico> buscar(
			@Param("consultaId") Long consultaId,
			@Param("doencaId") Long doencaId,
			@Param("severidade") String severidade,
			@Param("confirmado") String confirmado,
			Pageable pageable
	);

}
