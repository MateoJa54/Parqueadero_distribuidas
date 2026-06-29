package ec.edu.espe.usuarios.config;

import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.security.RolesBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Siembra los roles BASE del sistema al arrancar, solo si aun no existen.
 *
 * No reemplaza ni elimina roles creados a mano: el administrador puede seguir
 * creando roles adicionales libremente. Esto garantiza que {@code CLIENTE}
 * (rol por defecto del registro) y {@code ADMIN}/{@code ROOT} existan siempre.
 */
@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class RolesSeeder implements ApplicationRunner {

    private static final Map<String, String> DESCRIPCIONES = Map.of(
            RolesBase.ROOT, "Super usuario con acceso total al sistema",
            RolesBase.ADMIN, "Gestion de zonas, espacios, vehiculos, roles-usuarios y asignaciones",
            RolesBase.RECAUDADOR, "Cobro de tickets (sin permisos asignados todavia)",
            RolesBase.CLIENTE, "Usuario final: sus datos, sus vehiculos y lectura del catalogo",
            RolesBase.INVITADO, "Acceso publico minimo");

    private final RolRepositorio rolRepositorio;

    @Override
    public void run(ApplicationArguments args) {
        RolesBase.BASE.forEach(nombre -> {
            if (!rolRepositorio.existsByName(nombre)) {
                rolRepositorio.save(Rol.builder()
                        .name(nombre)
                        .description(DESCRIPCIONES.getOrDefault(nombre, nombre))
                        .active(true)
                        .build());
                log.info("Rol base sembrado: {}", nombre);
            }
        });
    }
}
