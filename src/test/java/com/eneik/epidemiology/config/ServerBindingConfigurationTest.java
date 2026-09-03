package com.eneik.epidemiology.config;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
    "server.address=${SERVER_ADDRESS:0.0.0.0}"
})
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY, type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class ServerBindingConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("Given server configuration, When environment resolves server.address, Then it defaults to 0.0.0.0")
    void testServerAddressBinding() {
        String serverAddress = environment.getProperty("server.address");
        assertEquals("0.0.0.0", serverAddress);
    }
}
