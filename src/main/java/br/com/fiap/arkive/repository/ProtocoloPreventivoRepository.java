package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.ProtocoloPreventivo;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface ProtocoloPreventivoRepository extends JpaRepository<ProtocoloPreventivo, Long> {

	@Query("""
			select p from ProtocoloPreventivo p
			where (:nome is null or lower(p.nome) like lower(concat('%', :nome, '%')))
			and (:tipo is null or p.tipo = :tipo)
			and (:especieId is null or p.especie.id = :especieId)
			and (:racaId is null or p.raca.id = :racaId)
			and (:ativo is null or p.ativo = :ativo)
			""")
	Page<ProtocoloPreventivo> buscar(
			@Param("nome") String nome,
			@Param("tipo") String tipo,
			@Param("especieId") Long especieId,
			@Param("racaId") Long racaId,
			@Param("ativo") String ativo,
			Pageable pageable
	);

}
