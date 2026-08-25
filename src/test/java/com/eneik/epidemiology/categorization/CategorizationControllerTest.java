package com.eneik.epidemiology.categorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CategorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RootCauseCategorizationService categorizationService;

    @Test
    @DisplayName("Given an authenticated user, When POST /api/v1/categorization/{streamName}, Then invokes service and returns categorized count")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCategorizeStream() throws Exception {
        when(categorizationService.categorizeReviewConcerns("reviewConcerns")).thenReturn(13);

        mockMvc.perform(post("/api/v1/categorization/reviewConcerns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorizedCount").value(13));

        verify(categorizationService, times(1)).categorizeReviewConcerns("reviewConcerns");
    }

    @Test
    @DisplayName("Given an unauthenticated user, When POST /api/v1/categorization/{streamName}, Then returns 401")
    void testCategorizeStreamUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/categorization/reviewConcerns"))
                .andExpect(status().isUnauthorized());
    }
}
