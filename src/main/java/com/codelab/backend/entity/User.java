package com.codelab.backend.entity;


import com.codelab.backend.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails, OAuth2User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;           // null for OAuth2-only users

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;           // false until email is verified

    @Column
    private String provider;           // "local" or "google"

    @Column
    private String providerId;         // Google's user ID, if OAuth2

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Stores Google's attributes temporarily — NOT persisted to DB
    @Transient                                                     // ← new
    private Map<String, Object> attributes;                        // ← new

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- UserDetails interface methods ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;   // We use email as the unique login identifier
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return enabled;  // Spring Security checks this — unverified users can't log in
    }

        // ── OAuth2User methods ───────────────────────────────────────

        @Override
        public Map<String, Object> getAttributes() {                   // ← new
            return attributes != null ? attributes : Map.of();
        }

        @Override
        public String getName() {                                      // ← new
            return email;
        }

}


///*
//
//Why does user implement UserDetails?
//
//ky user implements karta hai UserDetails kyunki Spring Security ke liye humein ek user model chahiye hota hai jo uske authentication aur authorization ke liye zaroori information provide kare. UserDetails interface ko implement karke hum apne User entity ko Spring Security ke context mein use kar sakte hain.
//
//*** UserDetails important hai implement karne ke liye : yes or no? ***
//Yes, agar aap Spring Security use kar rahe hain to UserDetails interface ko implement karna zaroori hai. Isse aap apne User entity ko Spring Security ke authentication process mein seamlessly integrate kar sakte hain.
//
//
//
//*/