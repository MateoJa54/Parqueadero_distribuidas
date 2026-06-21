package ec.edu.espe.usuarios.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.espe.usuarios.dtos.RolRequestDto;
import ec.edu.espe.usuarios.dtos.RolResponseDto;
import ec.edu.espe.usuarios.services.RolServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolServicio rolServicio;

    @GetMapping
    public ResponseEntity<List<RolResponseDto>> listarRoles() {
        return ResponseEntity.ok(rolServicio.listarRoles());
    }

    @PostMapping
    public ResponseEntity<RolResponseDto> crearRol(@Valid @RequestBody RolRequestDto request) {
        return new ResponseEntity<>(rolServicio.crearRol(request), HttpStatus.CREATED);
    }

    @GetMapping("/{idRol}")
    public ResponseEntity<RolResponseDto> obtenerRol(@PathVariable UUID idRol) {
        return ResponseEntity.ok(rolServicio.obtenerRol(idRol));
    }

    @PutMapping("/{idRol}")
    public ResponseEntity<RolResponseDto> actualizarRol(@PathVariable UUID idRol,
            @Valid @RequestBody RolRequestDto request) {
        return ResponseEntity.ok(rolServicio.actualizarRol(idRol, request));
    }

    @DeleteMapping("/{idRol}")
    public ResponseEntity<RolResponseDto> eliminarRol(@PathVariable UUID idRol) {
        return ResponseEntity.ok(rolServicio.eliminarRol(idRol));
    }
}
