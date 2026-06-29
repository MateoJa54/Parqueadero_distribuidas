package ec.edu.espe.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import ec.edu.espe.usuarios.security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class UsuariosApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsuariosApplication.class, args);
	}

}
