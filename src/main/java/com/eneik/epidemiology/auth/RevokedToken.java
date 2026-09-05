package com.eneik.epidemiology.auth;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "revoked_at", nullable = false)
    private OffsetDateTime revokedAt;

    public RevokedToken() {
    }

    public RevokedToken(String token) {
        this.token = token;
        this.revokedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }
}
