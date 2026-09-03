package br.com.fiap.arkive.service.speech;

import br.com.fiap.arkive.config.AzureSpeechProperties;
import br.com.fiap.arkive.domain.transcricao.SupportedAudioFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioConversionServiceTest {

	@Test
	void wavTemporarioEhRemovidoAoFecharPreparedAudio() throws Exception {
		AudioConversionService service = new AudioConversionService(new AzureSpeechProperties());
		byte[] audio = { 'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'A', 'V', 'E' };
		Path wavPath;

		try (PreparedAudio preparedAudio = service.prepararWav(audio, SupportedAudioFormat.WAV)) {
			wavPath = preparedAudio.wavFile();
			assertTrue(Files.exists(wavPath));
			assertArrayEquals(audio, Files.readAllBytes(wavPath));
		}

		assertFalse(Files.exists(wavPath));
	}
}
