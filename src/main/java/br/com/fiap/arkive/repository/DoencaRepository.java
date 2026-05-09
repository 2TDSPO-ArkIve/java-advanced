package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Doenca;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!local-nodb")
public interface DoencaRepository extends JpaRepository<Doenca, Long> {
}
