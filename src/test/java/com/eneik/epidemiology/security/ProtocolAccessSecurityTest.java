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
class ProtocolAccessSecurityTest {

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
    @DisplayName("Given user with RESEARCHER role, When accessing protocols, Then 200 OK access granted")
    void testProtocolAccess_ResearcherRole_Granted() throws Exception {
        String token = "valid_researcher_token";
        configureMockToken(token, "researcher_user", "RESEARCHER");

        Mockito.when(documentRepository.fullTextSearch(isNull(), Mockito.eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given user with EPIDEMIOLOGIST role, When accessing protocols, Then 200 OK access granted")
    void testProtocolAccess_EpidemiologistRole_Granted() throws Exception {
        String token = "valid_epidemiologist_token";
        configureMockToken(token, "epidemiologist_user", "EPIDEMIOLOGIST");

        Mockito.when(documentRepository.fullTextSearch(isNull(), Mockito.eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given user with ADMIN role, When accessing protocols, Then 200 OK access granted")
    void testProtocolAccess_AdminRole_Granted() throws Exception {
        String token = "valid_admin_token";
        configureMockToken(token, "admin_user", "ADMIN");

        Mockito.when(documentRepository.fullTextSearch(isNull(), Mockito.eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given user with USER role, When attempting to access protocols, Then 403 Forbidden access blocked")
    void testProtocolAccess_UserRole_Forbidden() throws Exception {
        String token = "valid_user_token";
        configureMockToken(token, "regular_user", "USER");

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Given unauthenticated request, When attempting to access protocols, Then 401 Unauthorized")
    void testProtocolAccess_Unauthenticated_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/protocols"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }
}
