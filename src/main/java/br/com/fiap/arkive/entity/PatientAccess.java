package br.com.fiap.arkive.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ARKIVE_PATIENT_ACCESS")
public class PatientAccess {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_PATIENT_ACCESS")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_USUARIO", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(name = "TP_ACESSO", nullable = false, length = 20)
	private PatientAccessRole accessRole;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_CONCEDIDO_POR", nullable = false)
	private Usuario grantedBy;

	@Column(name = "DT_CONCEDIDO", nullable = false)
	private LocalDateTime grantedAt = LocalDateTime.now();

	@Column(name = "DT_EXPIRACAO")
	private LocalDateTime expiresAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_REVOGADO_POR")
	private Usuario revokedBy;

	@Column(name = "DT_REVOGADO")
	private LocalDateTime revokedAt;

	public boolean isEffectiveAt(LocalDateTime now) {
		return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Animal getAnimal() { return animal; }
	public void setAnimal(Animal animal) { this.animal = animal; }
	public Usuario getUsuario() { return usuario; }
	public void setUsuario(Usuario usuario) { this.usuario = usuario; }
	public PatientAccessRole getAccessRole() { return accessRole; }
	public void setAccessRole(PatientAccessRole accessRole) { this.accessRole = accessRole; }
	public Usuario getGrantedBy() { return grantedBy; }
	public void setGrantedBy(Usuario grantedBy) { this.grantedBy = grantedBy; }
	public LocalDateTime getGrantedAt() { return grantedAt; }
	public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
	public LocalDateTime getExpiresAt() { return expiresAt; }
	public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
	public Usuario getRevokedBy() { return revokedBy; }
	public void setRevokedBy(Usuario revokedBy) { this.revokedBy = revokedBy; }
	public LocalDateTime getRevokedAt() { return revokedAt; }
	public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
