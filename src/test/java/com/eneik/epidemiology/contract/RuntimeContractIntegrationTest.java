package com.eneik.epidemiology.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class RuntimeContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCreateAndGetContract() throws Exception {
        String contractJson = """
            {
                "id": "9b58412d",
                "name": "Runtime Contract 9b58412d",
                "isActive": true
            }
            """;

        mockMvc.perform(post("/api/runtime-contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(contractJson))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value("9b58412d"));

        mockMvc.perform(get("/api/runtime-contracts/9b58412d"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value("9b58412d"))
               .andExpect(jsonPath("$.name").value("Runtime Contract 9b58412d"))
               .andExpect(jsonPath("$.isActive").value(true));
    }
}
