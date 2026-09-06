package mdro.platform.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"security.jwt.secret=test-only-secret-that-is-long-enough-for-hmac",
		"security.jwt.expiration=3600"
})
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
