package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_VS_PROTOCOLO_PREV")
public class ProtocoloPreventivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_PROTOCOLO")
	private Long id;

	@Column(name = "NM_PROTOCOLO", nullable = false, length = 200)
	private String nome;

	@Column(name = "TP_PROTOCOLO", nullable = false, length = 50)
	private String tipo;

	@Column(name = "DS_PROTOCOLO", length = 500)
	private String descricao;

	@Column(name = "NR_INTERVALO_DIAS", nullable = false)
	private Integer intervaloDias;

	@Column(name = "NR_IDADE_MIN_MESES", nullable = false)
	private Integer idadeMinMeses = 0;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_ESPECIE")
	private Especie especie;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_RACA")
	private Raca raca;

	@Column(name = "ST_ATIVO", nullable = false, length = 1)
	private String ativo = "S";

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Integer getIntervaloDias() {
		return intervaloDias;
	}

	public void setIntervaloDias(Integer intervaloDias) {
		this.intervaloDias = intervaloDias;
	}

	public Integer getIdadeMinMeses() {
		return idadeMinMeses;
	}

	public void setIdadeMinMeses(Integer idadeMinMeses) {
		this.idadeMinMeses = idadeMinMeses;
	}

	public Especie getEspecie() {
		return especie;
	}

	public void setEspecie(Especie especie) {
		this.especie = especie;
	}

	public Raca getRaca() {
		return raca;
	}

	public void setRaca(Raca raca) {
		this.raca = raca;
	}

	public String getAtivo() {
		return ativo;
	}

	public void setAtivo(String ativo) {
		this.ativo = ativo;
	}

}
