package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AdesaoPrescricao;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.PatientAccessRole;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.PatientAccessRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

@Service
@Profile("!local-nodb")
public class ClinicalAccessService {

	private final AnimalResponsavelRepository animalResponsavelRepository;
	private final ConsultaRepository consultaRepository;
	private final VeterinarioService veterinarioService;
	private final PatientAccessRepository patientAccessRepository;

	@Autowired
	public ClinicalAccessService(
			AnimalResponsavelRepository animalResponsavelRepository,
			ConsultaRepository consultaRepository,
			VeterinarioService veterinarioService,
			ObjectProvider<PatientAccessRepository> patientAccessRepositoryProvider
	) {
		this(animalResponsavelRepository, consultaRepository, veterinarioService,
				patientAccessRepositoryProvider.getIfAvailable());
	}

	public ClinicalAccessService(
			AnimalResponsavelRepository animalResponsavelRepository,
			ConsultaRepository consultaRepository,
			VeterinarioService veterinarioService,
			PatientAccessRepository patientAccessRepository
	) {
		this.animalResponsavelRepository = animalResponsavelRepository;
		this.consultaRepository = consultaRepository;
		this.veterinarioService = veterinarioService;
		this.patientAccessRepository = patientAccessRepository;
	}

	public ClinicalAccessService(AnimalResponsavelRepository animalResponsavelRepository,
			ConsultaRepository consultaRepository, VeterinarioService veterinarioService) {
		this(animalResponsavelRepository, consultaRepository, veterinarioService,
				(PatientAccessRepository) null);
	}

	public void exigirEscritaClinicaVeterinario(UsuarioPrincipal principal, Consulta consulta) {
		exigirPrincipal(principal);
		if (!TipoUsuario.VETERINARIO.equals(principal.getTipoUsuario())) {
			throw new AccessDeniedException("Operacao clinica permitida apenas ao veterinario responsavel.");
		}
		if (principal.getVeterinarioId() == null || consulta.getVeterinario() == null
				|| !Objects.equals(principal.getVeterinarioId(), consulta.getVeterinario().getId())) {
			throw new AccessDeniedException("Veterinario nao autorizado para esta consulta.");
		}
	}

	public void exigirLeituraConsulta(UsuarioPrincipal principal, Consulta consulta) {
		exigirPrincipal(principal);
		if (podeLerConsulta(principal, consulta)) {
			return;
		}
		throw new AccessDeniedException("Usuario nao autorizado para esta consulta.");
	}

	public void exigirLeituraAnimal(UsuarioPrincipal principal, Animal animal) {
		exigirPrincipal(principal);
		if (podeLerAnimal(principal, animal)) {
			return;
		}
		throw new AccessDeniedException("Usuario nao autorizado para este animal.");
	}

	public void exigirGestaoVinculosAnimal(UsuarioPrincipal principal, Animal animal) {
		exigirPrincipal(principal);
		if (TipoUsuario.RESPONSAVEL.equals(principal.getTipoUsuario())) {
			throw new AccessDeniedException("Gestão de responsáveis permitida apenas a veterinários e administradores autorizados.");
		}
		exigirLeituraAnimal(principal, animal);
	}

	public void exigirCompartilhamentoAnimal(UsuarioPrincipal principal, Animal animal) {
		exigirPrincipal(principal);
		if (!TipoUsuario.SYSADMIN.equals(principal.getTipoUsuario())
				&& !TipoUsuario.ADMIN_CLINICA.equals(principal.getTipoUsuario())
				&& !TipoUsuario.VETERINARIO.equals(principal.getTipoUsuario())
				&& !TipoUsuario.RESPONSAVEL.equals(principal.getTipoUsuario())) {
			throw new AccessDeniedException("Perfil nao autorizado a compartilhar pacientes.");
		}
		exigirLeituraAnimal(principal, animal);
	}

	public void exigirCriacaoCompartilhamentoAnimal(UsuarioPrincipal principal, Animal animal) {
		exigirPrincipal(principal);
		if (!TipoUsuario.VETERINARIO.equals(principal.getTipoUsuario())
				&& !TipoUsuario.RESPONSAVEL.equals(principal.getTipoUsuario())) {
			throw new AccessDeniedException("Somente veterinarios e responsaveis podem compartilhar pacientes.");
		}
		exigirLeituraAnimal(principal, animal);
	}

	public void exigirEscritaDiagnosticoVeterinario(UsuarioPrincipal principal, Diagnostico diagnostico) {
		exigirEscritaClinicaVeterinario(principal, diagnostico.getConsulta());
	}

	public void exigirLeituraDiagnostico(UsuarioPrincipal principal, Diagnostico diagnostico) {
		exigirLeituraConsulta(principal, diagnostico.getConsulta());
	}

	public void exigirEscritaPrescricaoVeterinario(UsuarioPrincipal principal, Prescricao prescricao) {
		exigirEscritaClinicaVeterinario(principal, prescricao.getConsulta());
	}

	public void exigirLeituraPrescricao(UsuarioPrincipal principal, Prescricao prescricao) {
		exigirLeituraConsulta(principal, prescricao.getConsulta());
	}

	public void exigirRegistroAdesaoResponsavel(UsuarioPrincipal principal, Prescricao prescricao) {
		exigirPrincipal(principal);
		if (!TipoUsuario.RESPONSAVEL.equals(principal.getTipoUsuario()) || principal.getResponsavelId() == null) {
			throw new AccessDeniedException("Registro de adesao permitido apenas ao responsavel vinculado ao animal.");
		}
		if (prescricao.getConsulta() == null || prescricao.getConsulta().getAnimal() == null
				|| !temVinculoResponsavelAnimal(principal.getResponsavelId(), prescricao.getConsulta().getAnimal().getId())) {
			throw new AccessDeniedException("Responsavel nao autorizado para registrar adesao desta prescricao.");
		}
	}

	public void exigirLeituraAdesaoPrescricao(UsuarioPrincipal principal, AdesaoPrescricao adesao) {
		exigirPrincipal(principal);
		if (podeLerAdesaoPrescricao(principal, adesao)) {
			return;
		}
		throw new AccessDeniedException("Usuario nao autorizado para esta adesao de prescricao.");
	}

	public boolean podeLerConsulta(UsuarioPrincipal principal, Consulta consulta) {
		exigirPrincipal(principal);
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> true;
			case VETERINARIO -> (principal.getVeterinarioId() != null
					&& consulta.getVeterinario() != null
					&& Objects.equals(principal.getVeterinarioId(), consulta.getVeterinario().getId()))
					|| temAcessoExplicito(principal, consulta.getAnimal(), PatientAccessRole.VETERINARIO);
			case RESPONSAVEL -> principal.getResponsavelId() != null
					&& consulta.getAnimal() != null
					&& (temVinculoResponsavelAnimal(principal.getResponsavelId(), consulta.getAnimal().getId())
					|| temAcessoExplicito(principal, consulta.getAnimal(), PatientAccessRole.RESPONSAVEL));
			case ADMIN_CLINICA -> principal.getClinicaId() != null
					&& consulta.getClinica() != null
					&& Objects.equals(principal.getClinicaId(), consulta.getClinica().getId());
		};
	}

	public boolean podeLerAnimal(UsuarioPrincipal principal, Animal animal) {
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> true;
			case RESPONSAVEL -> principal.getResponsavelId() != null
					&& (temVinculoResponsavelAnimal(principal.getResponsavelId(), animal.getId())
					|| temAcessoExplicito(principal, animal, PatientAccessRole.RESPONSAVEL));
			case VETERINARIO -> principal.getVeterinarioId() != null
					&& "S".equals(animal.getAtivo())
					&& (animalCadastradoPeloVeterinario(animal, principal.getVeterinarioId())
					|| consultaRepository.existsConsultaDoVeterinarioParaAnimal(animal.getId(), principal.getVeterinarioId())
					|| animalAtivoDaClinicaDoVeterinario(animal, principal.getVeterinarioId())
					|| temAcessoExplicito(principal, animal, PatientAccessRole.VETERINARIO));
			case ADMIN_CLINICA -> principal.getClinicaId() != null
					&& animal.getClinica() != null
					&& Objects.equals(principal.getClinicaId(), animal.getClinica().getId());
		};
	}

	private boolean animalCadastradoPeloVeterinario(Animal animal, Long veterinarioId) {
		return animal.getVeterinarioCadastro() != null
				&& Objects.equals(veterinarioId, animal.getVeterinarioCadastro().getId());
	}

	private boolean animalAtivoDaClinicaDoVeterinario(Animal animal, Long veterinarioId) {
		Long clinicaId = veterinarioService.buscarClinicaId(veterinarioId);
		return clinicaId != null
				&& "S".equals(animal.getAtivo())
				&& animal.getClinica() != null
				&& Objects.equals(clinicaId, animal.getClinica().getId());
	}

	private boolean podeLerAdesaoPrescricao(UsuarioPrincipal principal, AdesaoPrescricao adesao) {
		Consulta consulta = adesao.getPrescricao() == null ? null : adesao.getPrescricao().getConsulta();
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> true;
			case VETERINARIO -> principal.getVeterinarioId() != null
					&& consulta != null
					&& consulta.getVeterinario() != null
					&& Objects.equals(principal.getVeterinarioId(), consulta.getVeterinario().getId());
			case RESPONSAVEL -> principal.getResponsavelId() != null
					&& adesao.getResponsavel() != null
					&& Objects.equals(principal.getResponsavelId(), adesao.getResponsavel().getId())
					&& consulta != null
					&& consulta.getAnimal() != null
					&& temVinculoResponsavelAnimal(principal.getResponsavelId(), consulta.getAnimal().getId());
			case ADMIN_CLINICA -> principal.getClinicaId() != null
					&& consulta != null
					&& consulta.getClinica() != null
					&& Objects.equals(principal.getClinicaId(), consulta.getClinica().getId());
		};
	}

	private boolean temVinculoResponsavelAnimal(Long responsavelId, Long animalId) {
		return animalId != null && animalResponsavelRepository.existsVinculoAtivoVigente(animalId, responsavelId, LocalDate.now());
	}

	public boolean temVinculoResponsavelAtivo(Long responsavelId, Long animalId) {
		return temVinculoResponsavelAnimal(responsavelId, animalId);
	}

	private boolean temAcessoExplicito(UsuarioPrincipal principal, Animal animal, PatientAccessRole role) {
		return patientAccessRepository != null && principal.getUsuarioId() != null && animal != null && animal.getId() != null
				&& patientAccessRepository.existsEffective(animal.getId(), principal.getUsuarioId(), role, java.time.LocalDateTime.now());
	}

	private void exigirPrincipal(UsuarioPrincipal principal) {
		if (principal == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
	}

}
