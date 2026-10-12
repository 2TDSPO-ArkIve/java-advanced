package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Notificacao;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Profile("!local-nodb")
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

	Optional<Notificacao> findByUsuarioIdAndChaveEvento(Long usuarioId, String chaveEvento);

	Optional<Notificacao> findByIdAndUsuarioId(Long id, Long usuarioId);

	Page<Notificacao> findByUsuarioIdOrderByDataCriacaoDescIdDesc(Long usuarioId, Pageable pageable);

	Page<Notificacao> findByUsuarioIdAndDataLeituraIsNullOrderByDataCriacaoDescIdDesc(Long usuarioId, Pageable pageable);

	long countByUsuarioIdAndDataLeituraIsNull(Long usuarioId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Notificacao n
			set n.dataLeitura = :dataLeitura
			where n.usuario.id = :usuarioId
			and n.dataLeitura is null
			""")
	int marcarTodasComoLidas(
			@Param("usuarioId") Long usuarioId,
			@Param("dataLeitura") LocalDateTime dataLeitura
	);
}

