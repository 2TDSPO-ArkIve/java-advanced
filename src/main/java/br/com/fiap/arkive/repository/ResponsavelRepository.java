package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Responsavel;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface ResponsavelRepository extends JpaRepository<Responsavel, Long> {

	@Query("""
			select r from Responsavel r
			where (:nome is null or lower(r.nome) like lower(concat('%', :nome, '%')))
			and (:documento is null or lower(r.documento) like lower(concat('%', :documento, '%')))
			and (:tipo is null or r.tipo = :tipo)
			and (:ativo is null or r.ativo = :ativo)
			""")
	Page<Responsavel> buscar(
			@Param("nome") String nome,
			@Param("documento") String documento,
			@Param("tipo") String tipo,
			@Param("ativo") String ativo,
			Pageable pageable
	);

}
