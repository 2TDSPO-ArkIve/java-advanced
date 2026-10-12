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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ARKIVE_USUARIO_DISPOSITIVO", uniqueConstraints = {
		@UniqueConstraint(name = "UQ_ARKIVE_USUARIO_DISP_TOKEN", columnNames = "DS_EXPO_PUSH_TOKEN"),
		@UniqueConstraint(name = "UQ_ARKIVE_USUARIO_DISP_INST", columnNames = {"ID_USUARIO", "DS_INSTALACAO_ID"})
})
public class UsuarioDispositivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_DISPOSITIVO")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_USUARIO", nullable = false)
	private Usuario usuario;

	@Column(name = "DS_EXPO_PUSH_TOKEN", nullable = false, length = 255, unique = true)
	private String expoPushToken;

	@Enumerated(EnumType.STRING)
	@Column(name = "TP_PLATAFORMA", nullable = false, length = 20)
	private PlataformaDispositivo plataforma;

	@Column(name = "DS_INSTALACAO_ID", nullable = false, length = 100)
	private String instalacaoId;

	@Column(name = "DT_CRIACAO", nullable = false, updatable = false)
	private LocalDateTime dataCriacao = LocalDateTime.now();

	@Column(name = "DT_ULTIMO_CONTATO", nullable = false)
	private LocalDateTime dataUltimoContato = LocalDateTime.now();

	@Column(name = "ST_ATIVO", nullable = false, columnDefinition = "CHAR(1)")
	private String ativo = "S";

	@Column(name = "DT_REVOGACAO")
	private LocalDateTime dataRevogacao;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Usuario getUsuario() { return usuario; }
	public void setUsuario(Usuario usuario) { this.usuario = usuario; }
	public String getExpoPushToken() { return expoPushToken; }
	public void setExpoPushToken(String expoPushToken) { this.expoPushToken = expoPushToken; }
	public PlataformaDispositivo getPlataforma() { return plataforma; }
	public void setPlataforma(PlataformaDispositivo plataforma) { this.plataforma = plataforma; }
	public String getInstalacaoId() { return instalacaoId; }
	public void setInstalacaoId(String instalacaoId) { this.instalacaoId = instalacaoId; }
	public LocalDateTime getDataCriacao() { return dataCriacao; }
	public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
	public LocalDateTime getDataUltimoContato() { return dataUltimoContato; }
	public void setDataUltimoContato(LocalDateTime dataUltimoContato) { this.dataUltimoContato = dataUltimoContato; }
	public String getAtivo() { return ativo; }
	public void setAtivo(String ativo) { this.ativo = ativo; }
	public LocalDateTime getDataRevogacao() { return dataRevogacao; }
	public void setDataRevogacao(LocalDateTime dataRevogacao) { this.dataRevogacao = dataRevogacao; }
}
