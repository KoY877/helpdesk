package com.helpdesk.backend.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.helpdesk.backend.model.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 255, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "user_order", nullable = false)
    private int order;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Returns the authorities granted to this user.
     * The single role is exposed as a {@code ROLE_<name>} authority so it works
     * with Spring Security's {@code hasRole}/{@code hasAnyRole} checks.
     *
     * @return a collection holding the user's role authority
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Map the enum role to the ROLE_ prefixed authority Spring Security expects
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Returns the username used by Spring Security, which is the user's email.
     *
     * @return the user's email
     */
    @Override
    public String getUsername() {
        // Authentication is based on the email, not a separate username
        return email;
    }

    /**
     * Indicates whether the account has expired.
     *
     * @return always {@code true}; account expiration is not used
     */
    @Override
    public boolean isAccountNonExpired() { return true; }

    /**
     * Indicates whether the account is locked.
     *
     * @return always {@code true}; account locking is not used
     */
    @Override
    public boolean isAccountNonLocked() { return true; }

    /**
     * Indicates whether the credentials have expired.
     *
     * @return always {@code true}; credential expiration is not used
     */
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /**
     * Indicates whether the account is enabled.
     *
     * @return always {@code true}; all accounts are enabled
     */
    @Override
    public boolean isEnabled() { return true; }
}
