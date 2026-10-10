package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.UsuarioDispositivo;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@Profile("!local-nodb")
public interface UsuarioDispositivoRepository extends JpaRepository<UsuarioDispositivo, Long> {

	Optional<UsuarioDispositivo> findByUsuarioIdAndInstalacaoId(Long usuarioId, String instalacaoId);

	Optional<UsuarioDispositivo> findByExpoPushToken(String expoPushToken);

	Optional<UsuarioDispositivo> findByIdAndUsuarioId(Long id, Long usuarioId);

	List<UsuarioDispositivo> findByUsuarioIdAndAtivoOrderByIdAsc(Long usuarioId, String ativo);
}

