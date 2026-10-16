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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ARKIVE_RELATO_ANIMAL")
public class RelatoAnimal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_RELATO")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_RESPONSAVEL_AUTOR", nullable = false)
	private Responsavel autor;

	@Lob
	@Column(name = "DS_RELATO", nullable = false)
	private String texto;

	@Lob
	@Column(name = "DS_RELATO_BUSCA", nullable = false)
	private String textoBusca;

	@Column(name = "DT_CRIACAO", nullable = false, updatable = false)
	private LocalDateTime dataCriacao;

	@Column(name = "DT_OCORRENCIA")
	private LocalDate dataOcorrencia;

	@Column(name = "ST_ORIGEM_TRANSCRICAO", nullable = false, columnDefinition = "CHAR(1)")
	private String origemTranscricao = "N";

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Animal getAnimal() {
		return animal;
	}

	public void setAnimal(Animal animal) {
		this.animal = animal;
	}

	public Responsavel getAutor() {
		return autor;
	}

	public void setAutor(Responsavel autor) {
		this.autor = autor;
	}

	public String getTexto() {
		return texto;
	}

	public void setTexto(String texto) {
		this.texto = texto;
	}

	public String getTextoBusca() {
		return textoBusca;
	}

	public void setTextoBusca(String textoBusca) {
		this.textoBusca = textoBusca;
	}

	public LocalDateTime getDataCriacao() {
		return dataCriacao;
	}

	public void setDataCriacao(LocalDateTime dataCriacao) {
		this.dataCriacao = dataCriacao;
	}

	public LocalDate getDataOcorrencia() {
		return dataOcorrencia;
	}

	public void setDataOcorrencia(LocalDate dataOcorrencia) {
		this.dataOcorrencia = dataOcorrencia;
	}

	public String getOrigemTranscricao() {
		return origemTranscricao;
	}

	public void setOrigemTranscricao(String origemTranscricao) {
		this.origemTranscricao = origemTranscricao;
	}
}
