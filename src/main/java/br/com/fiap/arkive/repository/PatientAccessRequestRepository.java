package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.PatientAccessRequest;
import br.com.fiap.arkive.entity.PatientAccessRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

@Profile("!local-nodb")
public interface PatientAccessRequestRepository extends JpaRepository<PatientAccessRequest, Long> {

	boolean existsByAnimalIdAndRequesterIdAndStatus(
			Long animalId,
			Long requesterId,
			PatientAccessRequestStatus status
	);

	@Query(value = """
			select r from PatientAccessRequest r
			join fetch r.animal a
			join fetch r.requester u
			join fetch u.veterinario v
			left join fetch r.resolvedBy
			left join fetch r.resultingAccess
			where u.id = :requesterId
			and (:status is null or r.status = :status)
			""", countQuery = """
			select count(r) from PatientAccessRequest r
			where r.requester.id = :requesterId
			and (:status is null or r.status = :status)
			""")
	Page<PatientAccessRequest> findForRequester(
			@Param("requesterId") Long requesterId,
			@Param("status") PatientAccessRequestStatus status,
			Pageable pageable
	);

	@Query(value = """
			select distinct r from PatientAccessRequest r
			join fetch r.animal a
			join fetch r.requester u
			join fetch u.veterinario v
			left join fetch r.resolvedBy
			left join fetch r.resultingAccess
			where exists (
				select ar from AnimalResponsavel ar
				where ar.animal = a
				and ar.responsavel.id = :responsavelId
				and ar.ativo = 'S'
				and ar.id.dataInicio <= :today
				and (ar.dataFim is null or ar.dataFim >= :today)
			)
			and (:status is null or r.status = :status)
			""", countQuery = """
			select count(r) from PatientAccessRequest r
			where exists (
				select ar from AnimalResponsavel ar
				where ar.animal = r.animal
				and ar.responsavel.id = :responsavelId
				and ar.ativo = 'S'
				and ar.id.dataInicio <= :today
				and (ar.dataFim is null or ar.dataFim >= :today)
			)
			and (:status is null or r.status = :status)
			""")
	Page<PatientAccessRequest> findForCurrentResponsible(
			@Param("responsavelId") Long responsavelId,
			@Param("today") LocalDate today,
			@Param("status") PatientAccessRequestStatus status,
			Pageable pageable
	);

	@Query("""
			select r from PatientAccessRequest r
			join fetch r.animal
			join fetch r.requester u
			join fetch u.veterinario
			left join fetch r.resolvedBy
			left join fetch r.resultingAccess
			where r.id = :id
			""")
	Optional<PatientAccessRequest> findDetailedById(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select r from PatientAccessRequest r
			join fetch r.animal
			join fetch r.requester u
			join fetch u.veterinario
			left join fetch r.resolvedBy
			left join fetch r.resultingAccess
			where r.id = :id
			""")
	Optional<PatientAccessRequest> findByIdForUpdate(@Param("id") Long id);
}
