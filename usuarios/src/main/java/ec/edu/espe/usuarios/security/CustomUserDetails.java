package ec.edu.espe.usuarios.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ec.edu.espe.usuarios.entidades.Usuario;
import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String password;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario, List<String> roles) {
        this.userId = usuario.getId();
        this.username = usuario.getUsername();
        this.password = usuario.getPasswordHash();
        this.active = usuario.isActive();
        this.authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
