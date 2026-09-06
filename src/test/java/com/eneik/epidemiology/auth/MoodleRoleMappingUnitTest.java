package com.eneik.epidemiology.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoodleRoleMappingUnitTest {

    private final AuthController authController = new AuthController(null, null, null, null, null, null);

    @ParameterizedTest
    @DisplayName("Given a Moodle role string containing 'Аспирант', When mapMoodleRole is evaluated, Then it returns 'RESEARCHER'")
    @CsvSource({
            "Аспирант, RESEARCHER",
            "аспирант, RESEARCHER",
            "Аспирантура, RESEARCHER",
            "Аспирант кафедры, RESEARCHER",
            "исследователь, RESEARCHER",
            "Исследователь, RESEARCHER",
            "student, RESEARCHER",
            "learner, RESEARCHER",
            "администратор, ADMIN",
            "Administrator, ADMIN",
            "старший научный сотрудник, EPIDEMIOLOGIST",
            "эпидемиолог, EPIDEMIOLOGIST",
            "unknown_role, USER"
    })
    void testMapMoodleRole(String inputRole, String expectedInternalRole) {
        String actualRole = authController.mapMoodleRole(inputRole);
        assertEquals(expectedInternalRole, actualRole);
    }

    @Test
    @DisplayName("Given null moodleRole, When mapMoodleRole is evaluated, Then it returns 'USER'")
    void testMapMoodleRole_NullInput() {
        assertEquals("USER", authController.mapMoodleRole(null));
    }
}
