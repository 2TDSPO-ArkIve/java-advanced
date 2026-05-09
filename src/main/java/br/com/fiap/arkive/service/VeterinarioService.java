package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.VeterinarioRequest;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class VeterinarioService {

	private final VeterinarioRepository veterinarioRepository;
	private final ClinicaService clinicaService;

	public VeterinarioService(VeterinarioRepository veterinarioRepository, ClinicaService clinicaService) {
		this.veterinarioRepository = veterinarioRepository;
		this.clinicaService = clinicaService;
	}

	@Transactional
	public VeterinarioResponse criar(VeterinarioRequest request) {
		Veterinario veterinario = new Veterinario();
		aplicarDados(veterinario, request, true);
		return VeterinarioResponse.fromEntity(veterinarioRepository.save(veterinario));
	}

	@Transactional(readOnly = true)
	public Page<VeterinarioResponse> listar(String nome, String crmv, Long clinicaId, String ativo, Pageable pageable) {
		validarAtivoQuandoInformado(ativo);
		return veterinarioRepository.buscar(
				vazioParaNulo(nome),
				vazioParaNulo(crmv),
				clinicaId,
				vazioParaNulo(ativo),
				pageable
		).map(VeterinarioResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public VeterinarioResponse buscarPorId(Long id) {
		return VeterinarioResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public VeterinarioResponse atualizar(Long id, VeterinarioRequest request) {
		Veterinario veterinario = buscarEntidade(id);
		aplicarDados(veterinario, request, false);
		return VeterinarioResponse.fromEntity(veterinarioRepository.save(veterinario));
	}

	@Transactional
	public void excluir(Long id) {
		Veterinario veterinario = buscarEntidade(id);
		veterinario.setAtivo("N");
		veterinarioRepository.save(veterinario);
	}

	private Veterinario buscarEntidade(Long id) {
		return veterinarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Veterinario nao encontrado."));
	}

	private void aplicarDados(Veterinario veterinario, VeterinarioRequest request, boolean criando) {
		String ativo = criando && request.ativo() == null ? "S" : request.ativo();
		validarAtivoQuandoInformado(ativo);
		Clinica clinica = request.clinicaId() == null ? null : clinicaService.buscarEntidade(request.clinicaId());
		veterinario.setNome(request.nome());
		veterinario.setCrmv(request.crmv());
		veterinario.setEspecialidade(request.especialidade());
		veterinario.setEmail(request.email());
		veterinario.setClinica(clinica);
		veterinario.setAtivo(ativo == null ? veterinario.getAtivo() : ativo);
	}

	private void validarAtivoQuandoInformado(String valor) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException("Ativo deve ser S ou N.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
