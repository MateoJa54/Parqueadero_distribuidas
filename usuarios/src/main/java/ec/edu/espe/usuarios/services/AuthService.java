package ec.edu.espe.usuarios.services;

import ec.edu.espe.usuarios.dtos.AuthResponseDto;
import ec.edu.espe.usuarios.dtos.LoginRequestDto;
import ec.edu.espe.usuarios.dtos.RegisterRequestDto;

public interface AuthService {

    AuthResponseDto login(LoginRequestDto request);

    AuthResponseDto register(RegisterRequestDto request);
}
