package ec.edu.espe.usuarios.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.entidades.UsuarioRolId;
import ec.edu.espe.usuarios.repositorios.PersonaRepositorio;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.security.RolesBase;
import ec.edu.espe.usuarios.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea un usuario ROOT inicial (bootstrap) si aun no existe.
 *
 * Sin este usuario seria imposible operar el sistema: todos los endpoints de
 * gestion (personas, usuarios, roles, asignaciones) exigen ADMIN/ROOT, y el
 * registro publico solo crea CLIENTE. Este seeder rompe ese circulo creando una
 * credencial administrativa conocida la primera vez que arranca el servicio.
 *
 * Credenciales por defecto:  usuario = root   /   contrasena via ADMIN_ROOT_PASSWORD
 * IMPORTANTE: definir ADMIN_ROOT_PASSWORD en un entorno real.
 */
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class BootstrapAdminSeeder implements ApplicationRunner {

    private static final String ROOT_DNI = "0000000000";
    private static final String ROOT_EMAIL = "root@parqueadero.local";
    private static final String ROOT_PHONE = "0000000000";

    @Value("${admin.root.username:root}")
    private String rootUsername;

    @Value("${admin.root.password:}")
    private String rootPassword;

    private final PersonaRepositorio personaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final RolRepositorio rolRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepositorio.existsByUsernameIgnoreCase(rootUsername)) {
            return;
        }

        if (rootPassword == null || rootPassword.isBlank()) {
            log.warn("No se creo el usuario ROOT: defina ADMIN_ROOT_PASSWORD para sembrar la credencial inicial.");
            return;
        }

        // Todo el sembrado ocurre en UNA sola transaccion: asi la Persona queda
        // 'managed' en el mismo contexto de persistencia y el Usuario (que
        // comparte su PK via @MapsId) puede referenciarla sin que Hibernate la
        // considere 'detached' (evita 'detached entity passed to persist').
        Persona persona = personaRepositorio.findByDni(ROOT_DNI)
                .orElseGet(() -> personaRepositorio.save(Persona.builder()
                        .firstName("Root")
                        .lastName("Sistema")
                        .dni(ROOT_DNI)
                        .email(ROOT_EMAIL)
                        .phone(ROOT_PHONE)
                        .address("N/A")
                        .nationality("N/A")
                        .active(true)
                        .build()));

        Usuario usuario = usuarioRepositorio.save(Usuario.builder()
                .persona(persona)
                .username(rootUsername)
                .passwordHash(PasswordUtil.hash(rootPassword))
                .active(true)
                .build());

        Rol rolRoot = rolRepositorio.findByName(RolesBase.ROOT)
                .orElseThrow(() -> new IllegalStateException(
                        "El rol ROOT no fue sembrado; revise el orden de los seeders"));

        usuarioRolRepositorio.save(UsuarioRol.builder()
                .id(new UsuarioRolId(usuario.getId(), rolRoot.getId()))
                .usuario(usuario)
                .rol(rolRoot)
                .active(true)
                .build());

        log.warn("Usuario ROOT inicial creado -> usuario: '{}'. Cambie la contrasena en produccion.",
                rootUsername);
    }
}
