package br.com.fiap.arkive.service;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Profile("!local-nodb")
public class ConsultaResumoPdfService {

	private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final String LOGO_PATH = "static/images/favicon.png";

	private final ConsultaService consultaService;
	private final ClinicalAccessService clinicalAccessService;
	private final DiagnosticoRepository diagnosticoRepository;
	private final PrescricaoRepository prescricaoRepository;
	private final AnimalResponsavelRepository animalResponsavelRepository;

	public ConsultaResumoPdfService(
			ConsultaService consultaService,
			ClinicalAccessService clinicalAccessService,
			DiagnosticoRepository diagnosticoRepository,
			PrescricaoRepository prescricaoRepository,
			AnimalResponsavelRepository animalResponsavelRepository
	) {
		this.consultaService = consultaService;
		this.clinicalAccessService = clinicalAccessService;
		this.diagnosticoRepository = diagnosticoRepository;
		this.prescricaoRepository = prescricaoRepository;
		this.animalResponsavelRepository = animalResponsavelRepository;
	}

	@Transactional(readOnly = true)
	public ConsultaResumoPdf gerarResumo(Long consultaId, UsuarioPrincipal principal) {
		Consulta consulta = consultaService.buscarEntidade(consultaId);
		clinicalAccessService.exigirLeituraConsulta(principal, consulta);
		if (!StatusConsulta.FI.getCodigo().equals(consulta.getStatus())) {
			throw new BusinessException("Resumo em PDF disponivel apenas para consulta finalizada.", HttpStatus.CONFLICT);
		}
		Diagnostico diagnostico = diagnosticoRepository.buscarDiagnosticosConfirmadosVeterinario(consultaId, PageRequest.of(0, 1))
				.stream()
				.findFirst()
				.orElseThrow(() -> new BusinessException("Consulta finalizada sem diagnostico confirmado pelo veterinario.", HttpStatus.CONFLICT));
		List<PrescricaoPdfData> prescricoes = prescricaoRepository.buscarPorConsulta(consultaId).stream()
				.map(PrescricaoPdfData::fromEntity)
				.toList();
		ConsultaResumoPdfData data = ConsultaResumoPdfData.fromEntities(
				consulta,
				diagnostico,
				prescricoes,
				responsavelPrincipalAtual(consulta)
		);
		return new ConsultaResumoPdf(render(data), filename(consulta));
	}

	private String responsavelPrincipalAtual(Consulta consulta) {
		Long animalId = consulta.getAnimal() == null ? null : consulta.getAnimal().getId();
		if (animalId == null) {
			return null;
		}
		List<AnimalResponsavel> responsaveis = animalResponsavelRepository.buscarResponsaveisPrincipaisAtivosVigentes(animalId, LocalDate.now());
		if (responsaveis.size() != 1 || responsaveis.get(0).getResponsavel() == null) {
			return null;
		}
		return responsaveis.get(0).getResponsavel().getNome();
	}

	private byte[] render(ConsultaResumoPdfData data) {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PdfRenderer renderer = new PdfRenderer(document, carregarLogo(document));
			renderer.render(data);
			renderer.close();
			document.save(output);
			return output.toByteArray();
		} catch (IOException ex) {
			throw new BusinessException("Nao foi possivel gerar o resumo em PDF.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private PDImageXObject carregarLogo(PDDocument document) throws IOException {
		ClassPathResource resource = new ClassPathResource(LOGO_PATH);
		if (!resource.exists()) {
			return null;
		}
		try (var inputStream = resource.getInputStream()) {
			return PDImageXObject.createFromByteArray(document, inputStream.readAllBytes(), "arkive-logo");
		}
	}

	private String filename(Consulta consulta) {
		String animal = consulta.getAnimal() == null ? "paciente" : consulta.getAnimal().getNome();
		String slug = Normalizer.normalize(nuloSeVazio(animal) == null ? "paciente" : animal, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-|-$)", "");
		if (slug.isBlank()) {
			slug = "paciente";
		}
		return "arkive-consulta-" + slug + "-" + consulta.getId() + ".pdf";
	}

	private static String nuloSeVazio(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	public record ConsultaResumoPdf(byte[] bytes, String filename) {
	}

	private record ConsultaResumoPdfData(
			String animalNome,
			String especie,
			String raca,
			String sexo,
			String castrado,
			LocalDateTime dataConsulta,
			String modalidade,
			String endereco,
			String motivo,
			String veterinarioNome,
			String crmv,
			String clinicaNome,
			String responsavelNome,
			String diagnostico,
			String severidade,
			String conclusao,
			List<PrescricaoPdfData> prescricoes
	) {
		private static ConsultaResumoPdfData fromEntities(
				Consulta consulta,
				Diagnostico diagnostico,
				List<PrescricaoPdfData> prescricoes,
				String responsavelNome
		) {
			Animal animal = consulta.getAnimal();
			Veterinario veterinario = consulta.getVeterinario();
			Clinica clinica = consulta.getClinica();
			return new ConsultaResumoPdfData(
					animal == null ? null : animal.getNome(),
					animal == null || animal.getEspecie() == null ? null : animal.getEspecie().getNome(),
					animal == null || animal.getRaca() == null ? null : animal.getRaca().getNome(),
					animal == null ? null : formatarSexo(animal.getSexo()),
					animal == null ? null : formatarCastrado(animal.getCastrado()),
					consulta.getDataHora(),
					formatarModalidade(consulta.getModalidade()),
					"PRESENCIAL".equals(consulta.getModalidade()) ? nuloSeVazio(consulta.getEndereco()) : null,
					consulta.getMotivo(),
					veterinario == null ? null : veterinario.getNome(),
					veterinario == null ? null : veterinario.getCrmv(),
					clinica == null ? null : clinica.getNome(),
					responsavelNome,
					diagnostico.getDiagnostico(),
					diagnostico.getSeveridade(),
					consulta.getObservacao(),
					List.copyOf(prescricoes)
			);
		}

		private static String formatarSexo(String sexo) {
			return switch (sexo == null ? "" : sexo) {
				case "M" -> "Macho";
				case "F" -> "Fêmea";
				default -> null;
			};
		}

		private static String formatarCastrado(String castrado) {
			return switch (castrado == null ? "" : castrado) {
				case "S" -> "Sim";
				case "N" -> "Não";
				default -> null;
			};
		}

		private static String formatarModalidade(String modalidade) {
			return switch (modalidade == null ? "" : modalidade) {
				case "PRESENCIAL" -> "Presencial";
				case "REMOTA" -> "Remota";
				default -> modalidade;
			};
		}
	}

	private record PrescricaoPdfData(
			String medicamento,
			String dosagem,
			String frequencia,
			String viaAdministracao,
			LocalDate dataInicio,
			LocalDate dataFim,
			String instrucoes
	) {
		private static PrescricaoPdfData fromEntity(Prescricao prescricao) {
			return new PrescricaoPdfData(
					prescricao.getMedicamento(),
					prescricao.getDosagem(),
					prescricao.getFrequencia(),
					prescricao.getViaAdministracao(),
					prescricao.getDataInicio(),
					prescricao.getDataFim(),
					prescricao.getInstrucoes()
			);
		}
	}

	private static final class PdfRenderer {

		private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
		private static final float MARGIN = 54;
		private static final float FOOTER_Y = 36;
		private static final float CONTENT_BOTTOM = 72;
		private static final float CONTENT_WIDTH = PAGE_SIZE.getWidth() - (MARGIN * 2);
		private static final Color PRIMARY = new Color(0x3F51B5);
		private static final Color TEXT = new Color(0x1F2937);
		private static final Color MUTED = new Color(0x6B7280);
		private static final Color LIGHT = new Color(0xEE, 0xF0, 0xFB);
		private static final Color SEPARATOR = new Color(0xDD, 0xE2, 0xEA);

		private final PDDocument document;
		private final PDImageXObject logo;
		private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
		private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

		private PDPageContentStream content;
		private float y;
		private int pageNumber;

		private PdfRenderer(PDDocument document, PDImageXObject logo) {
			this.document = document;
			this.logo = logo;
		}

		private void render(ConsultaResumoPdfData data) throws IOException {
			addPage();
			section("Paciente");
			field("Nome do paciente", data.animalNome());
			field("Espécie", data.especie());
			field("Raça", data.raca());
			field("Sexo", data.sexo());
			field("Castrado", data.castrado());
			field("Responsável", data.responsavelNome());

			section("Atendimento");
			field("Data da consulta", data.dataConsulta() == null ? null : DATA_HORA_FORMATTER.format(data.dataConsulta()));
			field("Modalidade", data.modalidade());
			field("Endere\u00e7o", data.endereco());
			paragraph("Motivo da consulta", data.motivo());

			section("Veterinário");
			field("Nome do veterinário", data.veterinarioNome());
			field("CRMV", data.crmv());
			field("Clínica", data.clinicaNome());

			section("Decisão Clínica Final");
			paragraph("Diagnóstico confirmado pelo veterinário", data.diagnostico());
			field("Severidade final", data.severidade());
			paragraph("Conclusão clínica / orientações finais", data.conclusao());

			if (!data.prescricoes().isEmpty()) {
				section("Medicações Registradas");
				for (PrescricaoPdfData prescricao : data.prescricoes()) {
					prescription(prescricao);
				}
			}
		}

		private void addPage() throws IOException {
			if (content != null) {
				content.close();
			}
			PDPage page = new PDPage(PAGE_SIZE);
			document.addPage(page);
			content = new PDPageContentStream(document, page);
			pageNumber++;
			y = PAGE_SIZE.getHeight() - MARGIN;
			header();
			footer();
		}

		private void header() throws IOException {
			float logoSize = 42;
			content.setNonStrokingColor(PRIMARY);
			content.addRect(MARGIN, y - logoSize, logoSize, logoSize);
			content.fill();
			if (logo != null) {
				content.drawImage(logo, MARGIN + 7, y - logoSize + 7, logoSize - 14, logoSize - 14);
			}
			text("ArkIve", MARGIN + logoSize + 14, y - 16, bold, 22, PRIMARY);
			text("RESUMO DO ATENDIMENTO VETERINÁRIO", MARGIN, y - 72, bold, 16, TEXT);
			content.setStrokingColor(PRIMARY);
			content.setLineWidth(2);
			content.moveTo(MARGIN, y - 86);
			content.lineTo(PAGE_SIZE.getWidth() - MARGIN, y - 86);
			content.stroke();
			y -= 114;
		}

		private void footer() throws IOException {
			content.setStrokingColor(SEPARATOR);
			content.setLineWidth(0.6f);
			content.moveTo(MARGIN, FOOTER_Y + 18);
			content.lineTo(PAGE_SIZE.getWidth() - MARGIN, FOOTER_Y + 18);
			content.stroke();
			text("Documento gerado pelo ArkIve a partir dos registros do atendimento veterinário.", MARGIN, FOOTER_Y, regular, 8, MUTED);
			String pageText = "Página " + pageNumber;
			float width = textWidth(pageText, regular, 8);
			text(pageText, PAGE_SIZE.getWidth() - MARGIN - width, FOOTER_Y, regular, 8, MUTED);
		}

		private void section(String title) throws IOException {
			ensureSpace(48);
			y -= 6;
			content.setNonStrokingColor(LIGHT);
			content.addRect(MARGIN, y - 17, CONTENT_WIDTH, 24);
			content.fill();
			text(title, MARGIN + 12, y - 9, bold, 12, PRIMARY);
			y -= 34;
		}

		private void field(String label, String value) throws IOException {
			value = nuloSeVazio(value);
			if (value == null) {
				return;
			}
			float valueX = MARGIN + 154;
			List<String> lines = wrap(value, regular, 10.5f, PAGE_SIZE.getWidth() - MARGIN - valueX);
			ensureSpace(18 + (lines.size() * 14));
			text(label, MARGIN, y, bold, 9, MUTED);
			float lineY = y;
			for (String line : lines) {
				text(line, valueX, lineY, regular, 10.5f, TEXT);
				lineY -= 14;
			}
			y = lineY - 4;
		}

		private void paragraph(String label, String value) throws IOException {
			value = nuloSeVazio(value);
			if (value == null) {
				return;
			}
			ensureSpace(44);
			text(label, MARGIN, y, bold, 9, MUTED);
			y -= 16;
			for (String paragraph : value.split("\\R", -1)) {
				List<String> lines = wrap(paragraph, regular, 10.5f, CONTENT_WIDTH);
				if (lines.isEmpty()) {
					y -= 8;
					continue;
				}
				for (String line : lines) {
					ensureSpace(16);
					text(line, MARGIN, y, regular, 10.5f, TEXT);
					y -= 14;
				}
				y -= 4;
			}
			y -= 8;
		}

		private void prescription(PrescricaoPdfData prescricao) throws IOException {
			ensureSpace(88);
			content.setStrokingColor(PRIMARY);
			content.setLineWidth(2);
			content.moveTo(MARGIN, y + 5);
			content.lineTo(MARGIN, y - 76);
			content.stroke();
			String medicamento = Objects.requireNonNullElse(nuloSeVazio(prescricao.medicamento()), "Medicação");
			for (String line : wrap(medicamento, bold, 12, CONTENT_WIDTH - 14)) {
				ensureSpace(18);
				text(line, MARGIN + 14, y, bold, 12, TEXT);
				y -= 15;
			}
			y -= 7;
			field("Dosagem", prescricao.dosagem());
			field("Frequência", prescricao.frequencia());
			field("Via de administração", prescricao.viaAdministracao());
			field("Data de início", prescricao.dataInicio() == null ? null : DATA_FORMATTER.format(prescricao.dataInicio()));
			field("Data de término", prescricao.dataFim() == null ? null : DATA_FORMATTER.format(prescricao.dataFim()));
			paragraph("Instruções", prescricao.instrucoes());
			y -= 8;
		}

		private void ensureSpace(float required) throws IOException {
			if (y - required < CONTENT_BOTTOM) {
				addPage();
			}
		}

		private void text(String value, float x, float textY, PDFont font, float fontSize, Color color) throws IOException {
			content.beginText();
			content.setFont(font, fontSize);
			content.setNonStrokingColor(color);
			content.newLineAtOffset(x, textY);
			content.showText(value.replace('\t', ' '));
			content.endText();
		}

		private List<String> wrap(String value, PDFont font, float fontSize, float maxWidth) throws IOException {
			String normalized = value == null ? "" : value.replace('\t', ' ').trim();
			if (normalized.isBlank()) {
				return List.of();
			}
			List<String> lines = new ArrayList<>();
			StringBuilder current = new StringBuilder();
			for (String word : normalized.split("\\s+")) {
				if (current.isEmpty()) {
					appendWrappedWord(lines, current, word, font, fontSize, maxWidth);
					continue;
				}
				String candidate = current + " " + word;
				if (textWidth(candidate, font, fontSize) <= maxWidth) {
					current.append(' ').append(word);
				} else {
					lines.add(current.toString());
					current.setLength(0);
					appendWrappedWord(lines, current, word, font, fontSize, maxWidth);
				}
			}
			if (!current.isEmpty()) {
				lines.add(current.toString());
			}
			return lines;
		}

		private void appendWrappedWord(
				List<String> lines,
				StringBuilder current,
				String word,
				PDFont font,
				float fontSize,
				float maxWidth
		) throws IOException {
			if (textWidth(word, font, fontSize) <= maxWidth) {
				current.append(word);
				return;
			}
			StringBuilder chunk = new StringBuilder();
			for (int i = 0; i < word.length(); i++) {
				String candidate = chunk.toString() + word.charAt(i);
				if (!chunk.isEmpty() && textWidth(candidate, font, fontSize) > maxWidth) {
					lines.add(chunk.toString());
					chunk.setLength(0);
				}
				chunk.append(word.charAt(i));
			}
			current.append(chunk);
		}

		private float textWidth(String value, PDFont font, float fontSize) throws IOException {
			return font.getStringWidth(value) / 1000 * fontSize;
		}

		private void close() throws IOException {
			if (content != null) {
				content.close();
				content = null;
			}
		}
	}
}
