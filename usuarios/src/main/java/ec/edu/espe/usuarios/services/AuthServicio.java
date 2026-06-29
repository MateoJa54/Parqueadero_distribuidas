package ec.edu.espe.usuarios.services;

import java.util.UUID;

import ec.edu.espe.usuarios.dtos.auth.AuthResponse;
import ec.edu.espe.usuarios.dtos.auth.LoginRequest;
import ec.edu.espe.usuarios.dtos.auth.PerfilResponse;
import ec.edu.espe.usuarios.dtos.auth.RegisterRequest;

/** Autenticacion: emision de tokens (login/register) y consulta del perfil propio. */
public interface AuthServicio {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    PerfilResponse perfil(UUID idUsuario);
}
