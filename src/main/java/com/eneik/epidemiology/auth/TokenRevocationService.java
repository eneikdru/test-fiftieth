package com.eneik.epidemiology.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;

    public TokenRevocationService(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Transactional
    public void revokeToken(String token) {
        if (token != null && !token.trim().isEmpty() && !revokedTokenRepository.existsByToken(token)) {
            revokedTokenRepository.save(new RevokedToken(token));
        }
    }

    @Transactional(readOnly = true)
    public boolean isTokenRevoked(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return revokedTokenRepository.existsByToken(token);
    }
}
