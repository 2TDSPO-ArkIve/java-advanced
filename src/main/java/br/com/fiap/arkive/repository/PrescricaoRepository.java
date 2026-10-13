package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Prescricao;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

@Profile("!local-nodb")
public interface PrescricaoRepository extends JpaRepository<Prescricao, Long> {

	interface CustoMedicamentoPorConsulta {
		Long getConsultaId();
		BigDecimal getValorMedicamentos();
		Long getQuantidadePrescricoesComValor();
	}

	@Query("""
			select p.consulta.id as consultaId,
			       sum(p.valorMedicamento) as valorMedicamentos,
			       count(p.valorMedicamento) as quantidadePrescricoesComValor
			from Prescricao p
			where p.consulta.animal.id = :animalId
			and p.consulta.status = 'FI'
			group by p.consulta.id
			""")
	List<CustoMedicamentoPorConsulta> somarCustosMedicamentosFinalizadosPorAnimal(
			@Param("animalId") Long animalId);

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

	@Query("""
			select p from Prescricao p
			where p.consulta.veterinario.id = :veterinarioId
			and (:consultaId is null or p.consulta.id = :consultaId)
			and (:medicamento is null or lower(p.medicamento) like lower(concat('%', :medicamento, '%')))
			""")
	Page<Prescricao> buscarParaVeterinario(
			@Param("veterinarioId") Long veterinarioId,
			@Param("consultaId") Long consultaId,
			@Param("medicamento") String medicamento,
			Pageable pageable
	);

	@Query("""
			select distinct p from Prescricao p
			join AnimalResponsavel ar on ar.animal = p.consulta.animal
			where ar.responsavel.id = :responsavelId
			and ar.ativo = 'S'
			and ar.id.dataInicio <= :dataAtual
			and (ar.dataFim is null or ar.dataFim >= :dataAtual)
			and (:consultaId is null or p.consulta.id = :consultaId)
			and (:medicamento is null or lower(p.medicamento) like lower(concat('%', :medicamento, '%')))
			""")
	Page<Prescricao> buscarParaResponsavel(
			@Param("responsavelId") Long responsavelId,
			@Param("dataAtual") java.time.LocalDate dataAtual,
			@Param("consultaId") Long consultaId,
			@Param("medicamento") String medicamento,
			Pageable pageable
	);

	@Query("""
			select p from Prescricao p
			where p.consulta.clinica.id = :clinicaId
			and (:consultaId is null or p.consulta.id = :consultaId)
			and (:medicamento is null or lower(p.medicamento) like lower(concat('%', :medicamento, '%')))
			""")
	Page<Prescricao> buscarParaClinica(
			@Param("clinicaId") Long clinicaId,
			@Param("consultaId") Long consultaId,
			@Param("medicamento") String medicamento,
			Pageable pageable
	);

	@Query("""
			select p from Prescricao p
			where p.consulta.id = :consultaId
			order by p.id asc
			""")
	List<Prescricao> buscarPorConsulta(@Param("consultaId") Long consultaId);

}
