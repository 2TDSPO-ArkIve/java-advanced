package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ARKIVE_PATIENT_ACCESS_REQ")
public class PatientAccessRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_ACCESS_REQUEST")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_USUARIO_SOLICITANTE", nullable = false)
	private Usuario requester;

	@Enumerated(EnumType.STRING)
	@Column(name = "ST_STATUS", nullable = false, length = 20)
	private PatientAccessRequestStatus status = PatientAccessRequestStatus.PENDING;

	@Column(name = "DT_SOLICITACAO", nullable = false, updatable = false)
	private LocalDateTime requestedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_USUARIO_RESOLVEDOR")
	private Usuario resolvedBy;

	@Column(name = "DT_RESOLUCAO")
	private LocalDateTime resolvedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_PATIENT_ACCESS")
	private PatientAccess resultingAccess;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Animal getAnimal() { return animal; }
	public void setAnimal(Animal animal) { this.animal = animal; }
	public Usuario getRequester() { return requester; }
	public void setRequester(Usuario requester) { this.requester = requester; }
	public PatientAccessRequestStatus getStatus() { return status; }
	public void setStatus(PatientAccessRequestStatus status) { this.status = status; }
	public LocalDateTime getRequestedAt() { return requestedAt; }
	public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
	public Usuario getResolvedBy() { return resolvedBy; }
	public void setResolvedBy(Usuario resolvedBy) { this.resolvedBy = resolvedBy; }
	public LocalDateTime getResolvedAt() { return resolvedAt; }
	public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
	public PatientAccess getResultingAccess() { return resultingAccess; }
	public void setResultingAccess(PatientAccess resultingAccess) { this.resultingAccess = resultingAccess; }
}
