package ec.edu.espe.usuarios.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.espe.usuarios.dtos.UsuarioRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.dtos.UsuarioUpdateDto;
import ec.edu.espe.usuarios.services.UsuarioServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioServicio usuarioServicio;

    // Gestion de usuarios: solo administradores.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ROOT')")
    public ResponseEntity<List<UsuarioResponseDto>> listarUsuarios() {
        return ResponseEntity.ok(usuarioServicio.listarUsuarios());
    }

    // El propio usuario puede ver sus datos; un admin puede ver los de cualquiera.
    @GetMapping("/{idUsuario}")
    @PreAuthorize("hasAnyRole('ADMIN','ROOT') or #idUsuario.toString() == authentication.name")
    public ResponseEntity<UsuarioResponseDto> obtenerUsuario(@PathVariable UUID idUsuario) {
        return ResponseEntity.ok(usuarioServicio.obtenerUsuario(idUsuario));
    }

    // Busqueda parcial: /api/v1/usuarios/buscar?username=...
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN','ROOT')")
    public ResponseEntity<List<UsuarioResponseDto>> buscarUsuarios(@RequestParam String username) {
        return ResponseEntity.ok(usuarioServicio.buscarUsuarios(username));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ROOT')")
    public ResponseEntity<UsuarioResponseDto> crearUsuario(@Valid @RequestBody UsuarioRequestDto request) {
        return new ResponseEntity<>(usuarioServicio.crearUsuario(request), HttpStatus.CREATED);
    }

    // El propio usuario puede actualizar sus datos; un admin los de cualquiera.
    @PutMapping("/{idUsuario}")
    @PreAuthorize("hasAnyRole('ADMIN','ROOT') or #idUsuario.toString() == authentication.name")
    public ResponseEntity<UsuarioResponseDto> actualizarUsuario(@PathVariable UUID idUsuario,
            @Valid @RequestBody UsuarioUpdateDto request) {
        return ResponseEntity.ok(usuarioServicio.actualizarUsuario(idUsuario, request));
    }

    @PatchMapping("/{idUsuario}/activar")
    @PreAuthorize("hasAnyRole('ADMIN','ROOT')")
    public ResponseEntity<UsuarioResponseDto> activarUsuario(@PathVariable UUID idUsuario) {
        return ResponseEntity.ok(usuarioServicio.activarUsuario(idUsuario));
    }

    @PatchMapping("/{idUsuario}/desactivar")
    @PreAuthorize("hasAnyRole('ADMIN','ROOT')")
    public ResponseEntity<UsuarioResponseDto> desactivarUsuario(@PathVariable UUID idUsuario) {
        return ResponseEntity.ok(usuarioServicio.desactivarUsuario(idUsuario));
    }
}
