package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.EspecieRequest;
import br.com.fiap.arkive.dto.response.EspecieResponse;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.EspecieRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class EspecieService {

	private final EspecieRepository especieRepository;

	public EspecieService(EspecieRepository especieRepository) {
		this.especieRepository = especieRepository;
	}

	@Transactional
	@CacheEvict(value = "especies", allEntries = true)
	public EspecieResponse criar(EspecieRequest request) {
		Especie especie = new Especie();
		especie.setNome(request.nome());
		especie.setAtivo("S");
		return EspecieResponse.fromEntity(especieRepository.save(especie));
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "especies", key = "'listar:' + (#nome == null ? '' : #nome) + ':' + #pageable")
	public Page<EspecieResponse> listar(String nome, Pageable pageable) {
		Page<Especie> especies = nome == null || nome.isBlank()
				? especieRepository.findAll(pageable)
				: especieRepository.findByNomeContainingIgnoreCase(nome, pageable);
		return especies.map(EspecieResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "especies", key = "'id:' + #id")
	public EspecieResponse buscarPorId(Long id) {
		return EspecieResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	@CacheEvict(value = "especies", allEntries = true)
	public EspecieResponse atualizar(Long id, EspecieRequest request) {
		Especie especie = buscarEntidade(id);
		especie.setNome(request.nome());
		return EspecieResponse.fromEntity(especieRepository.save(especie));
	}

	@Transactional
	@CacheEvict(value = "especies", allEntries = true)
	public void excluir(Long id) {
		Especie especie = buscarEntidade(id);
		especie.setAtivo("N");
		especieRepository.save(especie);
	}

	@Transactional(readOnly = true)
	public Especie buscarEntidade(Long id) {
		return especieRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Especie nao encontrada."));
	}

}
