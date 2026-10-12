package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.PatientShareToken;
import jakarta.persistence.LockModeType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Profile("!local-nodb")
public interface PatientShareTokenRepository extends JpaRepository<PatientShareToken, Long> {

	boolean existsByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select pst from PatientShareToken pst where pst.tokenHash = :tokenHash")
	Optional<PatientShareToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select pst from PatientShareToken pst where pst.id = :id")
	Optional<PatientShareToken> findByIdForUpdate(@Param("id") Long id);

	Optional<PatientShareToken> findByTokenHash(String tokenHash);

	List<PatientShareToken> findByAnimalIdOrderByCreatedAtDesc(Long animalId);
}
