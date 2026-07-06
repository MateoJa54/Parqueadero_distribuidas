package ec.edu.espe.zonas.security;

/**
 * Roles que pueden ADMINISTRAR zonas y espacios (crear/editar/activar/desactivar/
 * cambiar estado). La consulta (listar/obtener/disponibilidad) solo exige estar
 * autenticado, sin restriccion de rol.
 *
 * <p>Los nombres coinciden con los roles emitidos por el microservicio usuarios.
 */
public final class RolesZonas {

    /** Crear/editar/activar/desactivar zonas y espacios (catalogo). */
    public static final String PUEDE_ADMINISTRAR = "hasAnyRole('ADMIN','ROOT')";

    /** Ocupar/liberar un espacio: lo usa tambien el microservicio tickets al
     *  operar ingresos, pagos y anulaciones. */
    public static final String PUEDE_CAMBIAR_ESTADO = "hasAnyRole('ADMIN','ROOT','RECAUDADOR')";

    private RolesZonas() {
    }
}
