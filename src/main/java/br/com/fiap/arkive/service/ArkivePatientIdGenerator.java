package br.com.fiap.arkive.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ArkivePatientIdGenerator {

	private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
	private static final int RANDOM_BYTES = 16;
	private final SecureRandom secureRandom;

	public ArkivePatientIdGenerator() {
		this(new SecureRandom());
	}

	ArkivePatientIdGenerator(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
	}

	public String generate() {
		byte[] random = new byte[RANDOM_BYTES];
		secureRandom.nextBytes(random);
		String encoded = encodeBase32(random);
		return "ARK-PET-" + encoded.substring(0, 5)
				+ "-" + encoded.substring(5, 10)
				+ "-" + encoded.substring(10, 15)
				+ "-" + encoded.substring(15, 20)
				+ "-" + encoded.substring(20);
	}

	private String encodeBase32(byte[] input) {
		StringBuilder result = new StringBuilder(26);
		int buffer = 0;
		int bits = 0;
		for (byte value : input) {
			buffer = (buffer << 8) | (value & 0xff);
			bits += 8;
			while (bits >= 5) {
				result.append(ALPHABET[(buffer >> (bits - 5)) & 31]);
				bits -= 5;
			}
		}
		if (bits > 0) {
			result.append(ALPHABET[(buffer << (5 - bits)) & 31]);
		}
		return result.toString();
	}
}
