package ec.edu.espe.usuarios.entidades;

/**
 * Roles disponibles en el sistema del parqueadero (Opcion C).
 * <ul>
 *   <li>ADMINISTRADOR: control total del sistema (zonas, espacios, usuarios y roles).</li>
 *   <li>SUPERVISOR: supervisa operadores y reportes de zonas.</li>
 *   <li>OPERADOR: opera entradas/salidas y estados de espacios.</li>
 *   <li>CAJERO: cobra las tarifas de parqueo.</li>
 *   <li>CLIENTE: usuario final del parqueadero (dueno del vehiculo).</li>
 * </ul>
 */
public enum NombreRol {
    ADMINISTRADOR,
    SUPERVISOR,
    OPERADOR,
    CAJERO,
    CLIENTE
}
