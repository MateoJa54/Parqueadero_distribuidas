package ec.edu.espe.asignaciones.security;

/**
 * Roles que pueden ADMINISTRAR asignaciones vehiculo-propietario (crear, modificar,
 * activar/desactivar, ver trazabilidad). Consultar la propia flota tambien la
 * permite el dueno de esos vehiculos.
 *
 * <p>Los nombres coinciden con los roles emitidos por el microservicio usuarios.
 */
public final class RolesAsignaciones {

    public static final String PUEDE_ADMINISTRAR = "hasAnyRole('ADMIN','ROOT')";

    /** ADMIN/ROOT o el propio propietario consultando su flota. */
    public static final String PUEDE_VER_FLOTA =
            "hasAnyRole('ADMIN','ROOT') or #userId.toString() == authentication.name";

    /** Incluye RECAUDADOR: lo usa el microservicio tickets al registrar un
     *  ingreso, para confirmar que el vehiculo tiene una asignacion activa. */
    public static final String PUEDE_CONSULTAR_ASIGNACION_VEHICULO = "hasAnyRole('ADMIN','ROOT','RECAUDADOR')";

    private RolesAsignaciones() {
    }
}
