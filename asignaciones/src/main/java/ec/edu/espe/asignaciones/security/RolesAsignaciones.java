package ec.edu.espe.asignaciones.security;

/**
 * Roles y expresiones de autorizacion para el microservicio asignaciones.
 *
 * <p>Los nombres coinciden con los roles emitidos por el microservicio usuarios.
 * Spring Security los expone como authorities {@code ROLE_<nombre>}.
 */
public final class RolesAsignaciones {

    public static final String ROOT = "ROOT";
    public static final String ADMIN = "ADMIN";
    public static final String RECAUDADOR = "RECAUDADOR";

    /** Administrar asignaciones vehiculo-propietario: crear/modificar/(des)activar/trazabilidad. */
    public static final String PUEDE_ADMINISTRAR = "hasAnyRole('ADMIN','ROOT')";

    /** Consultar la asignacion activa de un vehiculo (lo usa tickets al registrar un ingreso). */
    public static final String PUEDE_CONSULTAR_VEHICULO = "hasAnyRole('RECAUDADOR','ADMIN','ROOT')";

    /** Consultar la flota de un propietario: el propio usuario, o ADMIN/ROOT. */
    public static final String PUEDE_CONSULTAR_FLOTA_PROPIA =
            "#userId.toString() == authentication.name or hasAnyRole('ADMIN','ROOT')";

    private RolesAsignaciones() {
    }
}
