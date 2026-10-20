package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Consulta;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Profile("!local-nodb")
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

	@Query("""
			select c from Consulta c
			join fetch c.animal
			join fetch c.veterinario
			where c.status = 'AG'
			and c.dataHora >= :inicio
			and c.dataHora <= :fim
			order by c.dataHora asc, c.id asc
			""")
	List<Consulta> buscarAgendadasParaLembrete(
			@Param("inicio") LocalDateTime inicio,
			@Param("fim") LocalDateTime fim
	);

	@Query("""
			select c from Consulta c
			join fetch c.animal
			join fetch c.veterinario
			left join fetch c.clinica
			where c.animal.id = :animalId
			and c.status = 'FI'
			order by c.dataHora desc, c.id desc
			""")
	List<Consulta> buscarCustosFinalizadosPorAnimal(@Param("animalId") Long animalId);

	@Query("""
			select c from Consulta c
			join fetch c.animal
			join fetch c.veterinario
			left join fetch c.clinica
			where c.animal.id = :animalId
			and c.veterinario.id = :veterinarioId
			and c.status = 'FI'
			order by c.dataHora desc, c.id desc
			""")
	List<Consulta> buscarCustosFinalizadosPorAnimalEVeterinario(
			@Param("animalId") Long animalId,
			@Param("veterinarioId") Long veterinarioId);

	@Query("""
			select c from Consulta c
			join fetch c.animal
			join fetch c.veterinario
			left join fetch c.clinica
			where c.veterinario.id = :veterinarioId
			and c.status = 'FI'
			order by c.dataHora desc, c.id desc
			""")
	List<Consulta> buscarCustosFinalizadosPorVeterinario(@Param("veterinarioId") Long veterinarioId);

	@Query("""
			select c from Consulta c
			join fetch c.veterinario
			left join fetch c.clinica
			where c.animal.id = :animalId
			order by c.dataHora desc, c.id desc
			""")
	List<Consulta> buscarHistoricoPorAnimal(@Param("animalId") Long animalId);

	@Query("""
			select c from Consulta c
			where (:consultaId is null or c.id = :consultaId)
			and (:pacienteNome is null or lower(c.animal.nome) like lower(concat('%', :pacienteNome, '%')))
			and (:arkivePatientId is null or lower(c.animal.arkivePatientId) = lower(:arkivePatientId))
			and (:dataHoraInicio is null or c.dataHora >= :dataHoraInicio)
			and (:dataHoraFim is null or c.dataHora <= :dataHoraFim)
			and (:animalId is null or c.animal.id = :animalId)
			and (:veterinarioId is null or c.veterinario.id = :veterinarioId)
			and (:clinicaId is null or c.clinica.id = :clinicaId)
			and (:status is null or c.status = :status)
			and (:modalidade is null or c.modalidade = :modalidade)
			""")
	Page<Consulta> buscar(
			@Param("consultaId") Long consultaId,
			@Param("pacienteNome") String pacienteNome,
			@Param("arkivePatientId") String arkivePatientId,
			@Param("dataHoraInicio") LocalDateTime dataHoraInicio,
			@Param("dataHoraFim") LocalDateTime dataHoraFim,
			@Param("animalId") Long animalId,
			@Param("veterinarioId") Long veterinarioId,
			@Param("clinicaId") Long clinicaId,
			@Param("status") String status,
			@Param("modalidade") String modalidade,
			Pageable pageable
	);

	default Page<Consulta> buscar(
			Long animalId,
			Long veterinarioId,
			Long clinicaId,
			String status,
			String modalidade,
			Pageable pageable
	) {
		return buscar(null, null, null, null, null, animalId, veterinarioId, clinicaId, status, modalidade, pageable);
	}

	@Query("""
			select c from Consulta c
			where exists (
				select ar.id from AnimalResponsavel ar
				where ar.animal = c.animal
				and ar.responsavel.id = :responsavelId
				and ar.ativo = 'S'
				and ar.id.dataInicio <= :dataAtual
				and (ar.dataFim is null or ar.dataFim >= :dataAtual)
			)
			and (:consultaId is null or c.id = :consultaId)
			and (:pacienteNome is null or lower(c.animal.nome) like lower(concat('%', :pacienteNome, '%')))
			and (:arkivePatientId is null or lower(c.animal.arkivePatientId) = lower(:arkivePatientId))
			and (:dataHoraInicio is null or c.dataHora >= :dataHoraInicio)
			and (:dataHoraFim is null or c.dataHora <= :dataHoraFim)
			and (:animalId is null or c.animal.id = :animalId)
			and (:veterinarioId is null or c.veterinario.id = :veterinarioId)
			and (:clinicaId is null or c.clinica.id = :clinicaId)
			and (:status is null or c.status = :status)
			and (:modalidade is null or c.modalidade = :modalidade)
			""")
	Page<Consulta> buscarParaResponsavel(
			@Param("responsavelId") Long responsavelId,
			@Param("dataAtual") LocalDate dataAtual,
			@Param("consultaId") Long consultaId,
			@Param("pacienteNome") String pacienteNome,
			@Param("arkivePatientId") String arkivePatientId,
			@Param("dataHoraInicio") LocalDateTime dataHoraInicio,
			@Param("dataHoraFim") LocalDateTime dataHoraFim,
			@Param("animalId") Long animalId,
			@Param("veterinarioId") Long veterinarioId,
			@Param("clinicaId") Long clinicaId,
			@Param("status") String status,
			@Param("modalidade") String modalidade,
			Pageable pageable
	);

	default Page<Consulta> buscarParaResponsavel(
			Long responsavelId,
			LocalDate dataAtual,
			Long animalId,
			Long veterinarioId,
			Long clinicaId,
			String status,
			String modalidade,
			Pageable pageable
	) {
		return buscarParaResponsavel(responsavelId, dataAtual, null, null, null, null, null,
				animalId, veterinarioId, clinicaId, status, modalidade, pageable);
	}

	@Query("""
			select count(c) > 0 from Consulta c
			where c.animal.id = :animalId
			and c.veterinario.id = :veterinarioId
			""")
	boolean existsConsultaDoVeterinarioParaAnimal(@Param("animalId") Long animalId, @Param("veterinarioId") Long veterinarioId);

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
