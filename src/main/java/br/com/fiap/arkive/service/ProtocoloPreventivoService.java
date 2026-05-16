package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.ProtocoloPreventivoRequest;
import br.com.fiap.arkive.dto.response.ProtocoloPreventivoResponse;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.ProtocoloPreventivo;
import br.com.fiap.arkive.entity.Raca;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.EspecieRepository;
import br.com.fiap.arkive.repository.ProtocoloPreventivoRepository;
import br.com.fiap.arkive.repository.RacaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Profile("!local-nodb")
public class ProtocoloPreventivoService {

	private static final Set<String> TIPOS = Set.of("VACINA", "VERMIFUGO", "CHECK-UP", "ANTIPARASITARIO");

	private final ProtocoloPreventivoRepository protocoloRepository;
	private final EspecieRepository especieRepository;
	private final RacaRepository racaRepository;

	public ProtocoloPreventivoService(
			ProtocoloPreventivoRepository protocoloRepository,
			EspecieRepository especieRepository,
			RacaRepository racaRepository
	) {
		this.protocoloRepository = protocoloRepository;
		this.especieRepository = especieRepository;
		this.racaRepository = racaRepository;
	}

	@Transactional
	public ProtocoloPreventivoResponse criar(ProtocoloPreventivoRequest request) {
		ProtocoloPreventivo protocolo = new ProtocoloPreventivo();
		aplicarDados(protocolo, request, true);
		return ProtocoloPreventivoResponse.fromEntity(protocoloRepository.save(protocolo));
	}

	@Transactional(readOnly = true)
	public Page<ProtocoloPreventivoResponse> listar(String nome, String tipo, Long especieId, Long racaId, String ativo, Pageable pageable) {
		validarTipoQuandoInformado(tipo);
		validarSNQuandoInformado(ativo, "Ativo");
		return protocoloRepository.buscar(vazioParaNulo(nome), vazioParaNulo(tipo), especieId, racaId, vazioParaNulo(ativo), pageable)
				.map(ProtocoloPreventivoResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public ProtocoloPreventivoResponse buscarPorId(Long id) {
		return ProtocoloPreventivoResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public ProtocoloPreventivoResponse atualizar(Long id, ProtocoloPreventivoRequest request) {
		ProtocoloPreventivo protocolo = buscarEntidade(id);
		aplicarDados(protocolo, request, false);
		return ProtocoloPreventivoResponse.fromEntity(protocoloRepository.save(protocolo));
	}

	@Transactional
	public void excluir(Long id) {
		ProtocoloPreventivo protocolo = buscarEntidade(id);
		protocolo.setAtivo("N");
		protocoloRepository.save(protocolo);
	}

	@Transactional(readOnly = true)
	public ProtocoloPreventivo buscarEntidade(Long id) {
		return protocoloRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Protocolo preventivo nao encontrado."));
	}

	private void aplicarDados(ProtocoloPreventivo protocolo, ProtocoloPreventivoRequest request, boolean criando) {
		String ativo = criando && request.ativo() == null ? "S" : request.ativo();
		validarTipoObrigatorio(request.tipo());
		validarSNQuandoInformado(ativo, "Ativo");
		Especie especie = request.especieId() == null ? null : buscarEspecie(request.especieId());
		Raca raca = request.racaId() == null ? null : buscarRaca(request.racaId());
		if (especie != null && raca != null && !raca.getEspecie().getId().equals(especie.getId())) {
			throw new BusinessException("Raca deve pertencer a especie informada.");
		}
		protocolo.setNome(request.nome());
		protocolo.setTipo(request.tipo());
		protocolo.setDescricao(request.descricao());
		protocolo.setIntervalo(request.intervalo());
		protocolo.setIdadeMin(request.idadeMin());
		protocolo.setEspecie(especie);
		protocolo.setRaca(raca);
		protocolo.setAtivo(ativo == null ? protocolo.getAtivo() : ativo);
	}

	private Especie buscarEspecie(Long id) {
		return especieRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Especie nao encontrada."));
	}

	private Raca buscarRaca(Long id) {
		return racaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Raca nao encontrada."));
	}

	private void validarTipoObrigatorio(String tipo) {
		if (!TIPOS.contains(tipo)) {
			throw new BusinessException("Tipo de protocolo invalido.");
		}
	}

	private void validarTipoQuandoInformado(String tipo) {
		if (tipo != null && !tipo.isBlank() && !TIPOS.contains(tipo)) {
			throw new BusinessException("Tipo de protocolo invalido.");
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
