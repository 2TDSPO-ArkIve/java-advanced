package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.PatientAccess;
import br.com.fiap.arkive.entity.PatientAccessRole;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Profile("!local-nodb")
public interface PatientAccessRepository extends JpaRepository<PatientAccess, Long> {

	@Query("""
			select count(pa) > 0 from PatientAccess pa
			where pa.animal.id = :animalId and pa.usuario.id = :usuarioId and pa.accessRole = :role
			and pa.revokedAt is null and (pa.expiresAt is null or pa.expiresAt > :now)
			""")
	boolean existsEffective(@Param("animalId") Long animalId, @Param("usuarioId") Long usuarioId,
			@Param("role") PatientAccessRole role, @Param("now") LocalDateTime now);

	@Query("""
			select pa from PatientAccess pa
			where pa.animal.id = :animalId and pa.usuario.id = :usuarioId and pa.accessRole = :role
			and pa.revokedAt is null and (pa.expiresAt is null or pa.expiresAt > :now)
			order by pa.grantedAt desc
			""")
	List<PatientAccess> findEffective(@Param("animalId") Long animalId, @Param("usuarioId") Long usuarioId,
			@Param("role") PatientAccessRole role, @Param("now") LocalDateTime now);

	List<PatientAccess> findByAnimalIdOrderByGrantedAtDesc(Long animalId);

	Optional<PatientAccess> findByIdAndAnimalId(Long id, Long animalId);
}
