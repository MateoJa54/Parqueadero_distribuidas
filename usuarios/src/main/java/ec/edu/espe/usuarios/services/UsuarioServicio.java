package ec.edu.espe.usuarios.services;

import java.util.List;
import java.util.UUID;

import ec.edu.espe.usuarios.dtos.UsuarioRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.dtos.UsuarioUpdateDto;

public interface UsuarioServicio {

    List<UsuarioResponseDto> listarUsuarios();

    UsuarioResponseDto obtenerUsuario(UUID idUsuario);

    UsuarioResponseDto crearUsuario(UsuarioRequestDto request);

    UsuarioResponseDto actualizarUsuario(UUID idUsuario, UsuarioUpdateDto request);

    UsuarioResponseDto activarUsuario(UUID idUsuario);

    UsuarioResponseDto desactivarUsuario(UUID idUsuario);

    // Busqueda parcial por username (case-insensitive).
    List<UsuarioResponseDto> buscarUsuarios(String username);
}
