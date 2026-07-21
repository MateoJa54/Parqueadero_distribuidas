package ec.edu.espe.usuarios.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import ec.edu.espe.usuarios.dtos.auth.AuthResponse;
import ec.edu.espe.usuarios.dtos.auth.LoginRequest;
import ec.edu.espe.usuarios.dtos.auth.PerfilResponse;
import ec.edu.espe.usuarios.dtos.auth.RefreshRequest;
import ec.edu.espe.usuarios.dtos.auth.RegisterRequest;
import ec.edu.espe.usuarios.dtos.auth.RegistroClienteRequest;
import ec.edu.espe.usuarios.dtos.auth.RegistroCompletoRequest;
import ec.edu.espe.usuarios.services.AuthServicio;

class AuthControllerTest {

    private AuthServicio authServicio;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authServicio = mock(AuthServicio.class);
        controller = new AuthController(authServicio);
    }

    private AuthResponse authResp() {
        return AuthResponse.builder().token("tok").refreshToken("ref").tokenType("Bearer")
                .idUsuario(UUID.randomUUID()).username("usr").roles(List.of()).build();
    }

    @Test
    @DisplayName("login retorna 200 con AuthResponse")
    void login() {
        LoginRequest req = new LoginRequest();
        req.setUsername("usr");
        req.setPassword("Pass1w");
        when(authServicio.login(req)).thenReturn(authResp());

        ResponseEntity<AuthResponse> resp = controller.login(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("tok", resp.getBody().getToken());
    }

    @Test
    @DisplayName("register retorna 201")
    void register() {
        RegisterRequest req = new RegisterRequest();
        req.setIdPersona(UUID.randomUUID());
        req.setUsername("usr");
        req.setPassword("Pass1word");
        when(authServicio.register(req)).thenReturn(authResp());

        ResponseEntity<AuthResponse> resp = controller.register(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    @DisplayName("registrarCliente retorna 201")
    void registrarCliente() {
        RegistroClienteRequest req = new RegistroClienteRequest();
        req.setDni("1713175071");
        req.setEmail("x@x.com");
        req.setUsername("usr");
        req.setPassword("Pass1word");
        when(authServicio.registrarCliente(req)).thenReturn(authResp());

        ResponseEntity<AuthResponse> resp = controller.registrarCliente(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    @DisplayName("registrarCompleto retorna 201")
    void registrarCompleto() {
        RegistroCompletoRequest req = new RegistroCompletoRequest();
        when(authServicio.registrarCompleto(req)).thenReturn(authResp());

        ResponseEntity<AuthResponse> resp = controller.registrarCompleto(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    @DisplayName("refresh retorna 200")
    void refresh() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("ref-tok");
        when(authServicio.refrescar(req)).thenReturn(authResp());

        ResponseEntity<AuthResponse> resp = controller.refresh(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("miPerfil extrae UUID de Authentication y retorna 200")
    void miPerfil() {
        UUID id = UUID.randomUUID();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(id.toString());

        PerfilResponse perfil = PerfilResponse.builder()
                .idUsuario(id).username("usr").active(true).roles(List.of()).build();
        when(authServicio.perfil(id)).thenReturn(perfil);

        ResponseEntity<PerfilResponse> resp = controller.miPerfil(auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(id, resp.getBody().getIdUsuario());
    }
}
