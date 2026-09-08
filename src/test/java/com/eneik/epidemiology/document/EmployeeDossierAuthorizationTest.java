package com.eneik.epidemiology.document;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@ActiveProfiles("test")
public class EmployeeDossierAuthorizationTest {

    @Autowired
    private EmployeeDocumentRepository documentRepository;

    @Autowired
    private DossierReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        documentRepository.deleteAll();
        reportRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    public void cleanup() {
        documentRepository.deleteAll();
        reportRepository.deleteAll();
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testStrictExactMatchingVsFuzzyFalsePositives() {
        // Create user with specific course "MATH-101"
        User user = new User("test_user", "hash", "USER", "test@test.com", "Test User", null, "Dept1", "MATH-101");
        userRepository.save(user);

        // Document requiring exact match "MATH-101"
        EmployeeDocument exactDoc = new EmployeeDocument();
        exactDoc.setEmployeeId("E1");
        exactDoc.setDocType("REPORT");
        exactDoc.setDocDate(LocalDate.now());
        exactDoc.setAccessCourse("MATH-101");

        exactDoc.setTitle("Test Doc");
        documentRepository.save(exactDoc);

        // Document that would false-positive match on "MATH-1" if using LIKE '%MATH-1%'
        // e.g. "MATH-1" is in "MATH-101", but the user courses is "MATH-101"
        // Wait, the vulnerability was: user has "MATH-1", doc requires "MATH-101" -> false positive access!
        // Let's create a user with "MATH-1" and a doc requiring "MATH-101"
        User userFuzzy = new User("fuzzy_user", "hash", "USER", "fuzzy@test.com", "Fuzzy User", null, "Dept1", "MATH-1");
        userRepository.save(userFuzzy);

        // As userFuzzy
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("fuzzy_user", null, List.of()));
        List<String> userFuzzyCourses = List.of("MATH-1");

        // The query uses: r.accessCourse IN :userCoursesList
        // so doc's accessCourse ("MATH-101") IN ["MATH-1"] -> false, NO ACCESS
        // If it was LIKE: '%MATH-101%' LIKE '%MATH-1%' -> NO. Wait. The old query was:
        // LOWER(CAST(:userCourses AS string)) LIKE LOWER(CONCAT('%', CAST(d.accessCourse AS string), '%'))
        // so "MATH-1, CS-202" LIKE "%MATH-101%" -> false.
        // Wait! Old query: userCourses LIKE '%accessCourse%'
        // If userCourses = "MATH-101", and doc accessCourse = "MATH-1",
        // then "MATH-101" LIKE "%MATH-1%" -> TRUE! False positive access!

        EmployeeDocument docRequiringMath1 = new EmployeeDocument();
        docRequiringMath1.setEmployeeId("E1");
        docRequiringMath1.setDocType("REPORT");
        docRequiringMath1.setDocDate(LocalDate.now());
        docRequiringMath1.setAccessCourse("MATH-1");

        docRequiringMath1.setTitle("Test Doc 2");
        documentRepository.save(docRequiringMath1);

        // As user with "MATH-101"
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test_user", null, List.of()));
        List<String> testUserCourses = List.of("MATH-101");

        // Execute query
        org.springframework.data.domain.Page<EmployeeDocument> results = documentRepository.searchEmployeeDocumentsSecure(
                null, null, null, null, null, null, null, false, "Dept1", testUserCourses, PageRequest.of(0, 10)
        );

        // testUser has "MATH-101". Should see exactDoc ("MATH-101"), but NOT docRequiringMath1 ("MATH-1")
        assertThat(results.getContent()).extracting(EmployeeDocument::getAccessCourse)
                .containsExactly("MATH-101");

        // Test Report Repository
        DossierReport exactReport = new DossierReport("E1", "SUMMARY", "COMPLETED", "MATH-101", 1, null);
        exactReport.setAccessCourse("MATH-101");
        reportRepository.save(exactReport);

        DossierReport fuzzyReport = new DossierReport("E1", "SUMMARY", "COMPLETED", "MATH-1", 1, null);
        fuzzyReport.setAccessCourse("MATH-1");
        reportRepository.save(fuzzyReport);

        org.springframework.data.domain.Page<DossierReport> reportResults = reportRepository.searchReportsSecure(
                null, false, "Dept1", testUserCourses, PageRequest.of(0, 10)
        );

        assertThat(reportResults.getContent()).extracting(DossierReport::getAccessCourse)
                .containsExactly("MATH-101");
    }
}
