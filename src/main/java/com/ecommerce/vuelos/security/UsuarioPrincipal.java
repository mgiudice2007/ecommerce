package com.ecommerce.vuelos.security;

import com.ecommerce.vuelos.model.Administrador;
import com.ecommerce.vuelos.model.Pasajero;
import com.ecommerce.vuelos.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Long getId() {
        return usuario.getId();
    }

    public boolean isAdministrador() {
        return usuario instanceof Administrador;
    }

    public boolean isPasajero() {
        return usuario instanceof Pasajero;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String rol = usuario instanceof Administrador ? "ROLE_ADMINISTRADOR" : "ROLE_PASAJERO";
        return List.of(new SimpleGrantedAuthority(rol));
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
