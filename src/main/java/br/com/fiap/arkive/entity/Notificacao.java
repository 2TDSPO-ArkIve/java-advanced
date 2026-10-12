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
@Table(name = "TB_ARKIVE_NOTIFICACAO", uniqueConstraints = @UniqueConstraint(
		name = "UQ_ARKIVE_NOTIF_EVENTO", columnNames = {"ID_USUARIO", "DS_CHAVE_EVENTO"}))
public class Notificacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_NOTIFICACAO")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_USUARIO", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(name = "TP_NOTIFICACAO", nullable = false, length = 50)
	private TipoNotificacao tipo;

	@Column(name = "DS_TITULO", nullable = false, length = 150)
	private String titulo;

	@Column(name = "DS_MENSAGEM", nullable = false, length = 500)
	private String mensagem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_ANIMAL")
	private Animal animal;

	@Enumerated(EnumType.STRING)
	@Column(name = "TP_RECURSO", length = 50)
	private TipoRecursoNotificacao tipoRecurso;

	@Column(name = "ID_RECURSO")
	private Long recursoId;

	@Column(name = "DS_CHAVE_EVENTO", length = 150)
	private String chaveEvento;

	@Column(name = "DT_CRIACAO", nullable = false, updatable = false)
	private LocalDateTime dataCriacao = LocalDateTime.now();

	@Column(name = "DT_LEITURA")
	private LocalDateTime dataLeitura;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Usuario getUsuario() { return usuario; }
	public void setUsuario(Usuario usuario) { this.usuario = usuario; }
	public TipoNotificacao getTipo() { return tipo; }
	public void setTipo(TipoNotificacao tipo) { this.tipo = tipo; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public String getMensagem() { return mensagem; }
	public void setMensagem(String mensagem) { this.mensagem = mensagem; }
	public Animal getAnimal() { return animal; }
	public void setAnimal(Animal animal) { this.animal = animal; }
	public TipoRecursoNotificacao getTipoRecurso() { return tipoRecurso; }
	public void setTipoRecurso(TipoRecursoNotificacao tipoRecurso) { this.tipoRecurso = tipoRecurso; }
	public Long getRecursoId() { return recursoId; }
	public void setRecursoId(Long recursoId) { this.recursoId = recursoId; }
	public String getChaveEvento() { return chaveEvento; }
	public void setChaveEvento(String chaveEvento) { this.chaveEvento = chaveEvento; }
	public LocalDateTime getDataCriacao() { return dataCriacao; }
	public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
	public LocalDateTime getDataLeitura() { return dataLeitura; }
	public void setDataLeitura(LocalDateTime dataLeitura) { this.dataLeitura = dataLeitura; }
}
