package ec.edu.espe.tickets.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import ec.edu.espe.tickets.config.TarifaProperties;
import ec.edu.espe.tickets.utils.CompatibilidadTipos;
import ec.edu.espe.tickets.utils.ReglaNegocioException;
import lombok.RequiredArgsConstructor;

/**
 * Calcula el valor a cobrar por un ticket.
 *
 * <p>Regla de cobro: POR HORA, redondeando hacia arriba la fraccion iniciada,
 * con un minimo de 1 hora. Ejemplos con tarifa 1.50:
 * <ul>
 *   <li>10 min  -> 1 hora  -> 1.50</li>
 *   <li>61 min  -> 2 horas -> 3.00</li>
 *   <li>120 min -> 2 horas -> 3.00</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CalculadoraTarifa {

    private static final long MINUTOS_POR_HORA = 60;
    private static final long HORAS_MINIMAS = 1;

    private final TarifaProperties tarifaProperties;

    /**
     * Devuelve la tarifa por hora para la combinacion vehiculo/espacio.
     * Cae a la tarifa por defecto si la combinacion no esta configurada.
     */
    public BigDecimal tarifaPorHora(String tipoVehiculo, String tipoEspacio) {
        String clave = CompatibilidadTipos.claveTarifa(tipoVehiculo, tipoEspacio);
        return tarifaProperties.getMatriz().getOrDefault(clave, tarifaProperties.getPorDefecto());
    }

    /**
     * Factor multiplicador de la tarifa segun el rol/categoria del propietario.
     * Cae al factor por defecto si el rol es nulo o no esta configurado.
     */
    public BigDecimal factorRol(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return tarifaProperties.getFactorRolDefecto();
        }
        return tarifaProperties.getFactorRol()
                .getOrDefault(categoria.trim().toUpperCase(), tarifaProperties.getFactorRolDefecto());
    }

    /**
     * Calcula el valor total del ticket entre el ingreso y la salida, aplicando la
     * tarifa diferenciada por rol/categoria del propietario.
     */
    public BigDecimal calcular(String tipoVehiculo, String tipoEspacio, String categoria,
            OffsetDateTime ingreso, OffsetDateTime salida) {
        if (salida.isBefore(ingreso)) {
            throw new ReglaNegocioException(
                    "La fecha de salida no puede ser anterior a la de ingreso");
        }
        long horas = horasCobrables(ingreso, salida);
        BigDecimal base = tarifaPorHora(tipoVehiculo, tipoEspacio)
                .multiply(BigDecimal.valueOf(horas));
        return base.multiply(factorRol(categoria)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Variante sin categoria: aplica el factor por defecto (compatibilidad). */
    public BigDecimal calcular(String tipoVehiculo, String tipoEspacio,
            OffsetDateTime ingreso, OffsetDateTime salida) {
        return calcular(tipoVehiculo, tipoEspacio, null, ingreso, salida);
    }

    /** Horas a cobrar: fraccion iniciada cuenta como hora completa; minimo 1. */
    public long horasCobrables(OffsetDateTime ingreso, OffsetDateTime salida) {
        long minutos = Duration.between(ingreso, salida).toMinutes();
        long horas = (long) Math.ceil((double) minutos / MINUTOS_POR_HORA);
        return Math.max(horas, HORAS_MINIMAS);
    }
}
