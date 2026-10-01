package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.usuario.CambiarRolRequest;
import com.ecommerce.vuelos.dto.usuario.UsuarioAdminResponse;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public List<UsuarioAdminResponse> listar() {
        return usuarioRepository.findAll(Sort.by("username")).stream()
                .map(UsuarioAdminResponse::desde)
                .toList();
    }

    /**
     * El rol se lee de la base en cada request (UsuarioDetailsService), asi que
     * el cambio rige enseguida aunque el usuario tenga un token viejo.
     * Si pasa a COMPRADOR no hace falta crearle el carrito: se crea solo la
     * primera vez que lo usa.
     */
    @Override
    @Transactional
    public UsuarioAdminResponse cambiarRol(Long usuarioId, CambiarRolRequest request, Long adminId) {
        if (usuarioId.equals(adminId)) {
            // Evita que el unico admin se saque el permiso a si mismo y nadie pueda administrar
            throw new BadRequestException("No podes cambiar tu propio rol");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        usuario.setRol(request.getRol());
        return UsuarioAdminResponse.desde(usuarioRepository.save(usuario));
    }
}
