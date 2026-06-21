package ec.edu.espe.usuarios.repositorios;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.espe.usuarios.entidades.Persona;

public interface PersonaRepositorio extends JpaRepository<Persona, UUID> {

    boolean existsByDni(String dni);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByDniAndIdNot(String dni, UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    // Busquedas pensadas para el sistema de tickets: identificar a la persona
    // por su cedula (exacta) o por coincidencia parcial de nombre / apellido.
    Optional<Persona> findByDni(String dni);

    List<Persona> findByLastNameContainingIgnoreCase(String lastName);

    List<Persona> findByFirstNameContainingIgnoreCase(String firstName);
}
