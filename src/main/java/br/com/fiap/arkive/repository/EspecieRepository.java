package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Especie;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!local-nodb")
public interface EspecieRepository extends JpaRepository<Especie, Long> {

	Page<Especie> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

}
