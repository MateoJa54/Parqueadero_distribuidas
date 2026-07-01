package ec.edu.espe.tickets.utils;

import java.text.Normalizer;
import java.util.Map;

/**
 * Reglas de compatibilidad entre el tipo de vehiculo (catalogo de vehiculos:
 * {@code Auto}, {@code Motocicleta}, {@code Camioneta}) y el tipo de espacio
 * (catalogo de zonas: {@code AUTO}, {@code MOTO}, {@code BUSETA}).
 *
 * <p>Compatibilidad ESTRICTA: cada vehiculo solo puede ocupar su tipo de espacio.
 */
public final class CompatibilidadTipos {

    private static final Map<String, String> VEHICULO_A_ESPACIO = Map.of(
            "AUTO", "AUTO",
            "MOTOCICLETA", "MOTO",
            "CAMIONETA", "BUSETA");

    private CompatibilidadTipos() {
    }

    /** Normaliza a mayusculas sin tildes ni espacios (ej. "Motocicleta" -> "MOTOCICLETA"). */
    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.trim().toUpperCase();
    }

    /** Devuelve el tipo de espacio requerido por un tipo de vehiculo, o null si es desconocido. */
    public static String espacioRequeridoPara(String tipoVehiculo) {
        return VEHICULO_A_ESPACIO.get(normalizar(tipoVehiculo));
    }

    /** Indica si un vehiculo puede estacionarse en un espacio del tipo dado. */
    public static boolean sonCompatibles(String tipoVehiculo, String tipoEspacio) {
        String esperado = espacioRequeridoPara(tipoVehiculo);
        return esperado != null && esperado.equals(normalizar(tipoEspacio));
    }

    /** Clave de tarifa "TIPOVEHICULO_TIPOESPACIO" (ej. "AUTO_AUTO"). */
    public static String claveTarifa(String tipoVehiculo, String tipoEspacio) {
        return normalizar(tipoVehiculo) + "_" + normalizar(tipoEspacio);
    }
}
