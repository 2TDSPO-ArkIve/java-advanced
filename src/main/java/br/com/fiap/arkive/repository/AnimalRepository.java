package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

@Profile("!local-nodb")
public interface AnimalRepository extends JpaRepository<Animal, Long> {

	@Query("""
			select a from Animal a
			where (:nome is null or lower(a.nome) like lower(concat('%', :nome, '%')))
			and (:especieId is null or a.especie.id = :especieId)
			and (:racaId is null or a.raca.id = :racaId)
			and (:clinicaId is null or a.clinica.id = :clinicaId)
			and (:ativo is null or a.ativo = :ativo)
			""")
	Page<Animal> buscar(
			@Param("nome") String nome,
			@Param("especieId") Long especieId,
			@Param("racaId") Long racaId,
			@Param("clinicaId") Long clinicaId,
			@Param("ativo") String ativo,
			Pageable pageable
	);

	@Query("""
			select distinct a from Animal a
			join AnimalResponsavel ar on ar.animal = a
			where ar.responsavel.id = :responsavelId
			and ar.ativo = 'S'
			and (ar.dataFim is null or ar.dataFim >= :dataAtual)
			and (:nome is null or lower(a.nome) like lower(concat('%', :nome, '%')))
			and (:especieId is null or a.especie.id = :especieId)
			and (:racaId is null or a.raca.id = :racaId)
			and (:clinicaId is null or a.clinica.id = :clinicaId)
			and (:ativo is null or a.ativo = :ativo)
			""")
	Page<Animal> buscarParaResponsavel(
			@Param("responsavelId") Long responsavelId,
			@Param("dataAtual") LocalDate dataAtual,
			@Param("nome") String nome,
			@Param("especieId") Long especieId,
			@Param("racaId") Long racaId,
			@Param("clinicaId") Long clinicaId,
			@Param("ativo") String ativo,
			Pageable pageable
	);

	@Query("""
			select distinct a from Animal a
			join Consulta c on c.animal = a
			where c.veterinario.id = :veterinarioId
			and (:nome is null or lower(a.nome) like lower(concat('%', :nome, '%')))
			and (:especieId is null or a.especie.id = :especieId)
			and (:racaId is null or a.raca.id = :racaId)
			and (:clinicaId is null or a.clinica.id = :clinicaId)
			and (:ativo is null or a.ativo = :ativo)
			""")
	Page<Animal> buscarParaVeterinario(
			@Param("veterinarioId") Long veterinarioId,
			@Param("nome") String nome,
			@Param("especieId") Long especieId,
			@Param("racaId") Long racaId,
			@Param("clinicaId") Long clinicaId,
			@Param("ativo") String ativo,
			Pageable pageable
	);

	@Query("""
			select a from Animal a
			where a.clinica.id = :clinicaId
			and a.ativo = 'S'
			and (:nome is null or lower(a.nome) like lower(concat('%', :nome, '%')))
			and (:especieId is null or a.especie.id = :especieId)
			and (:racaId is null or a.raca.id = :racaId)
			""")
	Page<Animal> buscarAtivosParaClinica(
			@Param("clinicaId") Long clinicaId,
			@Param("nome") String nome,
			@Param("especieId") Long especieId,
			@Param("racaId") Long racaId,
			Pageable pageable
	);

	long countByAtivo(String ativo);

	@Query("""
			select a.especie.nome, count(a)
			from Animal a
			where a.ativo = :ativo
			group by a.especie.nome
			order by count(a) desc, a.especie.nome asc
			""")
	java.util.List<Object[]> contarAtivosPorEspecie(@Param("ativo") String ativo);

}
