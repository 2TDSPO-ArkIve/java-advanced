package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Veterinario;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {

	@Query("""
			select v from Veterinario v
			where (:nome is null or lower(v.nome) like lower(concat('%', :nome, '%')))
			and (:crmv is null or lower(v.crmv) like lower(concat('%', :crmv, '%')))
			and (:clinicaId is null or v.clinica.id = :clinicaId)
			and (:ativo is null or v.ativo = :ativo)
			""")
	Page<Veterinario> buscar(
			@Param("nome") String nome,
			@Param("crmv") String crmv,
			@Param("clinicaId") Long clinicaId,
			@Param("ativo") String ativo,
			Pageable pageable
	);

}
