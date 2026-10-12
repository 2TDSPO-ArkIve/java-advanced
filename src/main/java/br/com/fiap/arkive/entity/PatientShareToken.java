package br.com.fiap.arkive.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ARKIVE_PATIENT_SHARE")
public class PatientShareToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_SHARE_TOKEN")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@Column(name = "DS_TOKEN_HASH", nullable = false, length = 64, unique = true, updatable = false)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_CRIADO_POR", nullable = false)
	private Usuario createdBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "TP_CONVITE", nullable = false, length = 20)
	private PatientAccessRole invitationType;

	@Column(name = "DT_CRIADO", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "DT_EXPIRACAO", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "DT_USADO")
	private LocalDateTime usedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_USADO_POR")
	private Usuario usedBy;

	@Column(name = "DT_REVOGADO")
	private LocalDateTime revokedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_REVOGADO_POR")
	private Usuario revokedBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_ACESSO_RESULTADO")
	private PatientAccess resultingAccess;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Animal getAnimal() { return animal; }
	public void setAnimal(Animal animal) { this.animal = animal; }
	public String getTokenHash() { return tokenHash; }
	public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
	public Usuario getCreatedBy() { return createdBy; }
	public void setCreatedBy(Usuario createdBy) { this.createdBy = createdBy; }
	public PatientAccessRole getInvitationType() { return invitationType; }
	public void setInvitationType(PatientAccessRole invitationType) { this.invitationType = invitationType; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getExpiresAt() { return expiresAt; }
	public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
	public LocalDateTime getUsedAt() { return usedAt; }
	public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
	public Usuario getUsedBy() { return usedBy; }
	public void setUsedBy(Usuario usedBy) { this.usedBy = usedBy; }
	public LocalDateTime getRevokedAt() { return revokedAt; }
	public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
	public Usuario getRevokedBy() { return revokedBy; }
	public void setRevokedBy(Usuario revokedBy) { this.revokedBy = revokedBy; }
	public PatientAccess getResultingAccess() { return resultingAccess; }
	public void setResultingAccess(PatientAccess resultingAccess) { this.resultingAccess = resultingAccess; }
}
