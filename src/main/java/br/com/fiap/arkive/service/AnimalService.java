package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Raca;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class AnimalService {

	private final AnimalRepository animalRepository;
	private final EspecieService especieService;
	private final RacaService racaService;
	private final ClinicaService clinicaService;

	public AnimalService(
			AnimalRepository animalRepository,
			EspecieService especieService,
			RacaService racaService,
			ClinicaService clinicaService
	) {
		this.animalRepository = animalRepository;
		this.especieService = especieService;
		this.racaService = racaService;
		this.clinicaService = clinicaService;
	}

	@Transactional
	public AnimalResponse criar(AnimalRequest request) {
		Animal animal = new Animal();
		aplicarDados(animal, request, true);
		return AnimalResponse.fromEntity(animalRepository.save(animal));
	}

	@Transactional(readOnly = true)
	public Page<AnimalResponse> listar(
			String nome,
			Long especieId,
			Long racaId,
			Long clinicaId,
			String ativo,
			Pageable pageable
	) {
		validarAtivoQuandoInformado(ativo);
		return animalRepository.buscar(
				vazioParaNulo(nome),
				especieId,
				racaId,
				clinicaId,
				vazioParaNulo(ativo),
				pageable
		).map(AnimalResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public AnimalResponse buscarPorId(Long id) {
		return AnimalResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public AnimalResponse atualizar(Long id, AnimalRequest request) {
		Animal animal = buscarEntidade(id);
		aplicarDados(animal, request, false);
		return AnimalResponse.fromEntity(animalRepository.save(animal));
	}

	@Transactional
	public void excluir(Long id) {
		Animal animal = buscarEntidade(id);
		animal.setAtivo("N");
		animalRepository.save(animal);
	}

	private Animal buscarEntidade(Long id) {
		return animalRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private void aplicarDados(Animal animal, AnimalRequest request, boolean criando) {
		String ativo = criando && request.ativo() == null ? "S" : request.ativo();
		String castrado = criando && request.castrado() == null ? "N" : request.castrado();
		validarAtivoQuandoInformado(ativo);
		validarSexoQuandoInformado(request.sexo());
		validarSNQuandoInformado(castrado, "Castrado");
		Especie especie = especieService.buscarEntidade(request.especieId());
		Raca raca = request.racaId() == null ? null : racaService.buscarEntidade(request.racaId());
		if (raca != null && !raca.getEspecie().getId().equals(especie.getId())) {
			throw new BusinessException("Raca deve pertencer a mesma especie do animal.");
		}
		Clinica clinica = request.clinicaId() == null ? null : clinicaService.buscarEntidade(request.clinicaId());
		animal.setNome(request.nome());
		animal.setSexo(request.sexo());
		animal.setCastrado(castrado == null ? animal.getCastrado() : castrado);
		animal.setEspecie(especie);
		animal.setRaca(raca);
		animal.setClinica(clinica);
		animal.setAtivo(ativo == null ? animal.getAtivo() : ativo);
	}

	private void validarAtivoQuandoInformado(String valor) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException("Ativo deve ser S ou N.");
		}
	}

	private void validarSexoQuandoInformado(String valor) {
		if (valor != null && !valor.isBlank() && !"M".equals(valor) && !"F".equals(valor)) {
			throw new BusinessException("Sexo deve ser M ou F.");
		}
	}

	private void validarSNQuandoInformado(String valor, String campo) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException(campo + " deve ser S ou N.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
