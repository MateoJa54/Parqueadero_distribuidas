package ec.edu.espe.tickets.security;

/**
 * Roles que pueden OPERAR tickets. El rol funcional es {@code RECAUDADOR}
 * ("Cobro de tickets"); {@code ADMIN} y {@code ROOT} lo incluyen por jerarquia.
 *
 * <p>Los nombres coinciden con los roles emitidos por el microservicio usuarios.
 * Spring Security los expone como authorities {@code ROLE_<nombre>}.
 */
public final class RolesTickets {

    public static final String ROOT = "ROOT";
    public static final String ADMIN = "ADMIN";
    public static final String RECAUDADOR = "RECAUDADOR";

    /** Expresion SpEL reutilizable para {@code @PreAuthorize}. */
    public static final String PUEDE_OPERAR = "hasAnyRole('RECAUDADOR','ADMIN','ROOT')";

    private RolesTickets() {
    }
}
