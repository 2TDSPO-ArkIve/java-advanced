package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Consulta;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Profile("!local-nodb")
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

	@Query("""
			select c from Consulta c
			where (:animalId is null or c.animal.id = :animalId)
			and (:veterinarioId is null or c.veterinario.id = :veterinarioId)
			and (:clinicaId is null or c.clinica.id = :clinicaId)
			and (:status is null or c.status = :status)
			and (:modalidade is null or c.modalidade = :modalidade)
			""")
	Page<Consulta> buscar(
			@Param("animalId") Long animalId,
			@Param("veterinarioId") Long veterinarioId,
			@Param("clinicaId") Long clinicaId,
			@Param("status") String status,
			@Param("modalidade") String modalidade,
			Pageable pageable
	);

	long countByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

	@Query("""
			select year(c.dataHora), month(c.dataHora), count(c)
			from Consulta c
			where c.dataHora >= :inicio and c.dataHora < :fim
			group by year(c.dataHora), month(c.dataHora)
			""")
	List<Object[]> contarPorMes(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

	@Query("""
			select c.status, count(c)
			from Consulta c
			group by c.status
			order by count(c) desc, c.status asc
			""")
	List<Object[]> contarPorStatus();

}
