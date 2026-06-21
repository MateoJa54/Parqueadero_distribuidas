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
import org.springframework.web.bind.annotation.RestController;

import ec.edu.espe.usuarios.dtos.AsignacionResponseDto;
import ec.edu.espe.usuarios.dtos.AsignarRolRequestDto;
import ec.edu.espe.usuarios.services.AsignacionServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints para la asignacion de roles a usuarios (tabla "user_role").
 */
@RestController
@RequestMapping("/api/v1/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionServicio asignacionServicio;

    @GetMapping
    public ResponseEntity<List<AsignacionResponseDto>> listarAsignaciones() {
        return ResponseEntity.ok(asignacionServicio.listarAsignaciones());
    }

    @PostMapping
    public ResponseEntity<AsignacionResponseDto> asignarRol(@Valid @RequestBody AsignarRolRequestDto request) {
        return new ResponseEntity<>(asignacionServicio.asignarRol(request), HttpStatus.CREATED);
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<AsignacionResponseDto>> listarRolesDeUsuario(@PathVariable UUID idUsuario) {
        return ResponseEntity.ok(asignacionServicio.listarRolesDeUsuario(idUsuario));
    }
}
