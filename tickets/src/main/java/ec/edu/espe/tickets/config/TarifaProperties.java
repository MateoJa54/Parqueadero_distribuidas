package ec.edu.espe.tickets.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Tarifas por hora del parqueadero, cargadas desde el archivo {@code .env}
 * (via spring-dotenv) y enlazadas en {@code application.yaml} bajo {@code tarifas}.
 *
 * <p>La clave de la matriz es {@code <TIPO_VEHICULO>_<TIPO_ESPACIO>} en mayusculas,
 * por ejemplo {@code AUTO_AUTO}, {@code MOTOCICLETA_MOTO}, {@code CAMIONETA_BUSETA}.
 * Mantener las tarifas aqui (y no en una tabla) evita el costo de una entidad
 * adicional para un catalogo pequeno y estable.
 */
@ConfigurationProperties(prefix = "tarifas")
@Getter
@Setter
public class TarifaProperties {

    /** Tarifas por combinacion vehiculo_espacio. */
    private Map<String, BigDecimal> matriz = new HashMap<>();

    /** Tarifa de respaldo si una combinacion no esta configurada. */
    private BigDecimal porDefecto = BigDecimal.ONE;

    /**
     * Factor multiplicador de la tarifa segun el rol/categoria del propietario
     * (tarifa diferenciada por rol). Clave = nombre del rol en MAYUSCULAS
     * (ej. INVITADO, CLIENTE). Un factor 1.00 es tarifa plena; 0.60 = 40% dcto.
     */
    private Map<String, BigDecimal> factorRol = new HashMap<>();

    /** Factor a aplicar cuando el rol no esta en {@link #factorRol}. */
    private BigDecimal factorRolDefecto = BigDecimal.ONE;
}
