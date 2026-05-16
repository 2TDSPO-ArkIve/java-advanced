package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_ARKIVE_PROTOCOLO_PREVENTIVO")
public class ProtocoloPreventivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_PROTOCOLO")
	private Long id;

	@Column(name = "NM_PROTOCOLO", nullable = false, length = 50)
	private String nome;

	@Column(name = "TP_PROTOCOLO", nullable = false, length = 50)
	private String tipo;

	@Lob
	@Column(name = "DS_PROTOCOLO")
	private String descricao;

	@Column(name = "NR_INTERVALO", nullable = false)
	private Integer intervalo;

	@Column(name = "NR_IDADE_MIN", nullable = false)
	private Integer idadeMin = 0;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_ESPECIE")
	private Especie especie;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_RACA")
	private Raca raca;

	@Column(name = "ST_ATIVO", nullable = false, columnDefinition = "CHAR(1)")
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

	public Integer getIntervalo() {
		return intervalo;
	}

	public void setIntervalo(Integer intervalo) {
		this.intervalo = intervalo;
	}

	public Integer getIdadeMin() {
		return idadeMin;
	}

	public void setIdadeMin(Integer idadeMin) {
		this.idadeMin = idadeMin;
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
