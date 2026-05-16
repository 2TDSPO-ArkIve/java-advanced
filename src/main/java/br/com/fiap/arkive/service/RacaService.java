package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RacaRequest;
import br.com.fiap.arkive.dto.response.RacaResponse;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Raca;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.RacaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Profile("!local-nodb")
public class RacaService {

	private static final Set<String> PORTES = Set.of("PEQUENO", "MEDIO", "GRANDE");

	private final RacaRepository racaRepository;
	private final EspecieService especieService;

	public RacaService(RacaRepository racaRepository, EspecieService especieService) {
		this.racaRepository = racaRepository;
		this.especieService = especieService;
	}

	@Transactional
	public RacaResponse criar(RacaRequest request) {
		Raca raca = new Raca();
		aplicarDados(raca, request);
		return RacaResponse.fromEntity(racaRepository.save(raca));
	}

	@Transactional(readOnly = true)
	public Page<RacaResponse> listar(String nome, Long especieId, Pageable pageable) {
		return racaRepository.buscar(vazioParaNulo(nome), especieId, pageable).map(RacaResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public RacaResponse buscarPorId(Long id) {
		return RacaResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public RacaResponse atualizar(Long id, RacaRequest request) {
		Raca raca = buscarEntidade(id);
		aplicarDados(raca, request);
		return RacaResponse.fromEntity(racaRepository.save(raca));
	}

	@Transactional
	public void excluir(Long id) {
		Raca raca = buscarEntidade(id);
		raca.setAtivo("N");
		racaRepository.save(raca);
	}

	@Transactional(readOnly = true)
	public Raca buscarEntidade(Long id) {
		return racaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Raca nao encontrada."));
	}

	private void aplicarDados(Raca raca, RacaRequest request) {
		validarPorte(request.porte());
		Especie especie = especieService.buscarEntidade(request.especieId());
		raca.setNome(request.nome());
		raca.setPorte(request.porte());
		raca.setEspecie(especie);
	}

	private void validarPorte(String porte) {
		if (porte != null && !porte.isBlank() && !PORTES.contains(porte)) {
			throw new BusinessException("Porte deve ser PEQUENO, MEDIO ou GRANDE.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
