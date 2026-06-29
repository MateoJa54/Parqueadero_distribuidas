package ec.edu.espe.usuarios.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.espe.usuarios.dtos.auth.AuthResponse;
import ec.edu.espe.usuarios.dtos.auth.LoginRequest;
import ec.edu.espe.usuarios.dtos.auth.PerfilResponse;
import ec.edu.espe.usuarios.dtos.auth.RegisterRequest;
import ec.edu.espe.usuarios.services.AuthServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServicio authServicio;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authServicio.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authServicio.register(request), HttpStatus.CREATED);
    }

    // "Ver mis datos": el id del usuario viaja en el token (principal = idUsuario).
    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> miPerfil(Authentication authentication) {
        UUID idUsuario = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(authServicio.perfil(idUsuario));
    }
}
