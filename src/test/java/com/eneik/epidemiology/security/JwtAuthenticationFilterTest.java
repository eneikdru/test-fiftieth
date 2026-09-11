package com.eneik.epidemiology.security;

import com.eneik.epidemiology.auth.TokenRevocationService;
import com.eneik.epidemiology.document.DocumentRepository;
import com.eneik.epidemiology.document.ProtocolController;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProtocolController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentRepository documentRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserService userService;

    private void configureMockToken(String token, String username, String role) {
        Mockito.when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        Mockito.when(tokenRevocationService.isTokenRevoked(token)).thenReturn(false);
        Mockito.when(jwtTokenProvider.getUsername(token)).thenReturn(username);
        Mockito.when(jwtTokenProvider.getRole(token)).thenReturn(role);
        Mockito.when(userService.resolveRoleByUsername(username)).thenReturn(java.util.Optional.of(role));
    }

    @Test
    @DisplayName("Given persistent user role, When request is processed, Then persistent role overrides token claims and grants 200 OK")
    void testAuthorizedRequest_PersistentRoleResolution_Granted200() throws Exception {
        String token = "mock_valid_token_persistent";
        configureMockToken(token, "persistent_user", "USER");
        Mockito.when(userService.resolveRoleByUsername("persistent_user")).thenReturn(java.util.Optional.of("RESEARCHER"));

        Mockito.when(documentRepository.fullTextSearch(isNull(), Mockito.eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given an authenticated request with standard 'Bearer' prefix, When requested, Then returns 200 OK")
    void testAuthorizedRequest_StandardBearerHeader_Granted200() throws Exception {
        String token = "mock_valid_token_1";
        configureMockToken(token, "researcher_user", "RESEARCHER");

        Mockito.when(documentRepository.fullTextSearch(isNull(), Mockito.eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given an authenticated request with lowercase 'bearer' prefix, When requested, Then returns 200 OK")
    void testAuthorizedRequest_LowercaseBearerHeader_Granted200() throws Exception {
        String token = "mock_valid_token_2";
        configureMockToken(token, "researcher_user", "RESEARCHER");

        Mockito.when(documentRepository.fullTextSearch(isNull(), Mockito.eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given a JWT token containing a lowercase role, When requested, Then role is normalized and returns 200 OK")
    void testAuthorizedRequest_LowercaseRoleInToken_NormalizedAndGranted200() throws Exception {
        String token = "mock_valid_token_3";
        configureMockToken(token, "researcher_user", "researcher");

        Mockito.when(documentRepository.fullTextSearch(isNull(), Mockito.eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given token passed as URL query parameter, When requested, Then resolves token and returns 200 OK")
    void testAuthorizedRequest_QueryParameterToken_Granted200() throws Exception {
        String token = "mock_valid_param_token";
        configureMockToken(token, "researcher_user", "RESEARCHER");

        Mockito.when(documentRepository.fullTextSearch(isNull(), Mockito.eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/protocols")
                .param("access_token", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given a valid token for a user that does not exist in DB, When requested, Then rejects with 401 Unauthorized")
    void testNonExistentUserInDatabase_Rejected401() throws Exception {
        String token = "token_non_existent_user";
        Mockito.when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        Mockito.when(tokenRevocationService.isTokenRevoked(token)).thenReturn(false);
        Mockito.when(jwtTokenProvider.getUsername(token)).thenReturn("ghost_user");
        Mockito.when(userService.resolveRoleByUsername("ghost_user")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Given a revoked token, When requested, Then rejects with 401 Unauthorized")
    void testRevokedToken_Rejected401() throws Exception {
        String token = "revoked_token";
        Mockito.when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        Mockito.when(tokenRevocationService.isTokenRevoked(token)).thenReturn(true);

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Given an invalid token, When requested, Then rejects with 401 Unauthorized")
    void testInvalidToken_Rejected401() throws Exception {
        String token = "invalid_token";
        Mockito.when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }
}
