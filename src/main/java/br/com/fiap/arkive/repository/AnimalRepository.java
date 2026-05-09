package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface AnimalRepository extends JpaRepository<Animal, Long> {

	@Query("""
			select a from Animal a
			where (:nome is null or lower(a.nome) like lower(concat('%', :nome, '%')))
			and (:especieId is null or a.especie.id = :especieId)
			and (:racaId is null or a.raca.id = :racaId)
			and (:clinicaCadastroId is null or a.clinicaCadastro.id = :clinicaCadastroId)
			and (:ativo is null or a.ativo = :ativo)
			""")
	Page<Animal> buscar(
			@Param("nome") String nome,
			@Param("especieId") Long especieId,
			@Param("racaId") Long racaId,
			@Param("clinicaCadastroId") Long clinicaCadastroId,
			@Param("ativo") String ativo,
			Pageable pageable
	);

}
