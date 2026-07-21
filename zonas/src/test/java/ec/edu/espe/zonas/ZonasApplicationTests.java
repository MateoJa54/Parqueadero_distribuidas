package ec.edu.espe.zonas;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requiere PostgreSQL y RabbitMQ; no disponible en CI. La logica se cubre con tests unitarios sin contexto Spring.")
@SpringBootTest
class ZonasApplicationTests {

	@Test
	void contextLoads() {
	}

}
