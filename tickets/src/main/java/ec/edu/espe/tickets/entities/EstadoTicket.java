package ec.edu.espe.tickets.entities;

/**
 * Ciclo de vida de un ticket de parqueadero.
 *
 * <ul>
 *   <li>{@link #ACTIVO}: el vehiculo esta dentro; el espacio quedo OCUPADO.</li>
 *   <li>{@link #PAGADO}: se registro la salida y el cobro; el espacio se libero.</li>
 *   <li>{@link #ANULADO}: correccion de un error humano; el espacio se libero,
 *       sin cobro. El registro nunca se borra (queda como evidencia).</li>
 * </ul>
 *
 * {@code PAGADO} y {@code ANULADO} son estados terminales.
 */
public enum EstadoTicket {
    ACTIVO,
    PAGADO,
    ANULADO
}
