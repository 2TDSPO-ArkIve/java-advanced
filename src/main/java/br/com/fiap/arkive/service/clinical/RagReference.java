package br.com.fiap.arkive.service.clinical;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonDeserialize(using = RagReference.Deserializer.class)
public record RagReference(
		String fonte,
		String titulo
) {
	public RagReference {
		fonte = normalizarOpcional(fonte);
		titulo = titulo == null ? null : titulo.strip();
	}

	private static String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.strip();
	}

	public static final class Deserializer extends JsonDeserializer<RagReference> {
		@Override
		public RagReference deserialize(JsonParser parser, DeserializationContext context) throws IOException {
			JsonNode node = parser.getCodec().readTree(parser);
			if (node == null || !node.isObject()) {
				return null;
			}
			return new RagReference(texto(node.get("fonte")), texto(node.get("titulo")));
		}

		private String texto(JsonNode node) {
			return node != null && node.isTextual() ? node.textValue() : null;
		}
	}
}
