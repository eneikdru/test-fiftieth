package com.eneik.epidemiology.privacy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DataErasureTokenRepositoryTest {

    @Autowired
    private DataErasureTokenRepository repository;

    @Test
    void testSaveAndRetrieveToken() {
        DataErasureToken token = new DataErasureToken();
        token.setSubjectId("test_user");
        token.setToken("secure_random_token_123");
        token.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        token.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15));

        DataErasureToken saved = repository.save(token);
        assertNotNull(saved.getId());

        Optional<DataErasureToken> retrieved = repository.findByToken("secure_random_token_123");
        assertTrue(retrieved.isPresent());
        assertEquals("test_user", retrieved.get().getSubjectId());
        assertFalse(retrieved.get().getUsed());
    }

    @Test
    void testMarkTokenAsUsedAtomically() {
        DataErasureToken token = new DataErasureToken();
        token.setSubjectId("test_user_2");
        token.setToken("secure_random_token_456");
        token.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        token.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15));

        DataErasureToken saved = repository.saveAndFlush(token);

        int updatedCount = repository.markTokenAsUsed(saved.getId());
        assertEquals(1, updatedCount);

        int updatedAgainCount = repository.markTokenAsUsed(saved.getId());
        assertEquals(0, updatedAgainCount);
    }
}