package br.com.fiap.arkive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local-nodb")
class ArkiveApplicationTests {

	@Test
	void contextLoads() {
	}

}
