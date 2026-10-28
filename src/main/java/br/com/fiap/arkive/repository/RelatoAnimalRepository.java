package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.RelatoAnimal;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

@Profile("!local-nodb")
public interface RelatoAnimalRepository extends JpaRepository<RelatoAnimal, Long> {

	@Query(value = """
			select r.id from RelatoAnimal r
			where r.animal.id = :animalId
			order by r.dataCriacao desc, r.id desc
			""",
			countQuery = "select count(r) from RelatoAnimal r where r.animal.id = :animalId")
	Page<Long> listarIdsPorAnimal(@Param("animalId") Long animalId, Pageable pageable);

	@Query(value = """
			SELECT r.ID_RELATO
			FROM TB_ARKIVE_RELATO_ANIMAL r
			WHERE r.ID_ANIMAL = :animalId
			  AND DBMS_LOB.INSTR(r.DS_RELATO_BUSCA, :buscaNormalizada, 1, 1) > 0
			ORDER BY r.DT_CRIACAO DESC, r.ID_RELATO DESC
			""",
			countQuery = """
			SELECT COUNT(*)
			FROM TB_ARKIVE_RELATO_ANIMAL r
			WHERE r.ID_ANIMAL = :animalId
			  AND DBMS_LOB.INSTR(r.DS_RELATO_BUSCA, :buscaNormalizada, 1, 1) > 0
			""",
			nativeQuery = true)
	Page<Long> buscarIdsPorAnimal(
			@Param("animalId") Long animalId,
			@Param("buscaNormalizada") String buscaNormalizada,
			Pageable pageable
	);

	@Query("""
			select r from RelatoAnimal r
			join fetch r.animal
			join fetch r.autor
			where r.id in :ids
			""")
	List<RelatoAnimal> buscarDetalhesPorIds(@Param("ids") Collection<Long> ids);
}
