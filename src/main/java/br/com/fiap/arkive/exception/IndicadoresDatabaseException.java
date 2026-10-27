package br.com.fiap.arkive.exception;

public class IndicadoresDatabaseException extends RuntimeException {

	private static final String SAFE_MESSAGE =
			"Nao foi possivel consultar os indicadores no Oracle.";

	public IndicadoresDatabaseException(Throwable cause) {
		super(SAFE_MESSAGE, cause);
	}

	public IndicadoresDatabaseException() {
		super(SAFE_MESSAGE);
	}
}
