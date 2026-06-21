package ec.edu.espe.usuarios.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import ec.edu.espe.usuarios.dtos.PersonaRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;
import ec.edu.espe.usuarios.services.PersonaServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaServicio personaServicio;

    @GetMapping
    public ResponseEntity<List<PersonaResponseDto>> listarPersonas() {
        return ResponseEntity.ok(personaServicio.listarPersonas());
    }

    @GetMapping("/{idPersona}")
    public ResponseEntity<PersonaResponseDto> obtenerPersona(@PathVariable UUID idPersona) {
        return ResponseEntity.ok(personaServicio.obtenerPersona(idPersona));
    }

    // Busqueda flexible: /api/v1/personas/buscar?dni=...  | ?apellido=...  | ?nombre=...
    @GetMapping("/buscar")
    public ResponseEntity<List<PersonaResponseDto>> buscarPersonas(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido) {
        return ResponseEntity.ok(personaServicio.buscarPersonas(dni, nombre, apellido));
    }

    @PostMapping
    public ResponseEntity<PersonaResponseDto> crearPersona(@Valid @RequestBody PersonaRequestDto request) {
        return new ResponseEntity<>(personaServicio.crearPersona(request), HttpStatus.CREATED);
    }

    @PutMapping("/{idPersona}")
    public ResponseEntity<PersonaResponseDto> actualizarPersona(@PathVariable UUID idPersona,
            @Valid @RequestBody PersonaRequestDto request) {
        return ResponseEntity.ok(personaServicio.actualizarPersona(idPersona, request));
    }

    @DeleteMapping("/{idPersona}")
    public ResponseEntity<PersonaResponseDto> eliminarPersona(@PathVariable UUID idPersona) {
        return ResponseEntity.ok(personaServicio.eliminarPersona(idPersona));
    }
}
