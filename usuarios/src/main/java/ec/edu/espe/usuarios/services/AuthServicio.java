package ec.edu.espe.usuarios.services;

import java.util.UUID;

import ec.edu.espe.usuarios.dtos.auth.AuthResponse;
import ec.edu.espe.usuarios.dtos.auth.LoginRequest;
import ec.edu.espe.usuarios.dtos.auth.PerfilResponse;
import ec.edu.espe.usuarios.dtos.auth.RefreshRequest;
import ec.edu.espe.usuarios.dtos.auth.RegisterRequest;
import ec.edu.espe.usuarios.dtos.auth.RegistroClienteRequest;
import ec.edu.espe.usuarios.dtos.auth.RegistroCompletoRequest;

/** Autenticacion: emision de tokens (login/register), refresco y consulta del perfil propio. */
public interface AuthServicio {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    /**
     * Auto-registro de cliente verificando identidad por (dni + email) sin exponer
     * el listado de personas. Resuelve la persona internamente y crea el usuario CLIENTE.
     */
    AuthResponse registrarCliente(RegistroClienteRequest request);

    /**
     * Auto-registro COMPLETO de cliente: crea la persona (identidad) y su usuario
     * (acceso) con rol CLIENTE en una sola transaccion.
     */
    AuthResponse registrarCompleto(RegistroCompletoRequest request);

    /** Emite un nuevo access token (y rota el refresh token) a partir de un refresh token valido. */
    AuthResponse refrescar(RefreshRequest request);

    PerfilResponse perfil(UUID idUsuario);
}
