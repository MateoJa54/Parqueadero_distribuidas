package ec.edu.espe.zonas.security;

/**
 * Roles que pueden operar sobre zonas y espacios.
 *
 * <p>ADMIN/ROOT administran el catalogo (crear/editar/activar/desactivar zonas
 * y espacios). RECAUDADOR ademas puede cambiar el estado de un espacio
 * (OCUPADO/DISPONIBLE), ya que eso ocurre como parte del flujo de tickets.
 *
 * <p>Los nombres coinciden con los roles emitidos por el microservicio usuarios.
 * Spring Security los expone como authorities {@code ROLE_<nombre>}.
 */
public final class RolesZonas {

    public static final String ROOT = "ROOT";
    public static final String ADMIN = "ADMIN";
    public static final String RECAUDADOR = "RECAUDADOR";

    /** Administracion del catalogo: crear/editar/activar/desactivar zonas y espacios. */
    public static final String PUEDE_ADMINISTRAR = "hasAnyRole('ADMIN','ROOT')";

    /** Cambiar el estado de un espacio (ocupar/liberar), ademas de administrar. */
    public static final String PUEDE_CAMBIAR_ESTADO = "hasAnyRole('RECAUDADOR','ADMIN','ROOT')";

    private RolesZonas() {
    }
}
