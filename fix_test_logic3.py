import os
# Now test logic is finally sound and code compiles. Let's fix EmployeeDossierControllerTest

test_file = "src/test/java/com/eneik/epidemiology/document/EmployeeDossierControllerTest.java"
with open(test_file, "r") as f:
    code = f.read()

setup_str = """        EmployeeDocument doc1 = new EmployeeDocument("EMP-999", "ORDER", "Приказ о назначении", LocalDate.of(2023, 1, 15), "Приказ №42");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-999", "REPORT", "Отчет по исследованию", LocalDate.of(2023, 6, 20), "Годовой отчет");
        doc2.setAccessDepartment("Эпидемиология");
        EmployeeDocument doc3 = new EmployeeDocument("EMP-888", "EXAM", "Экзамен", LocalDate.of(2023, 11, 10), "Оценка: отлично");
        EmployeeDocument doc4 = new EmployeeDocument("EMP-777", "Ivanov", "REPORT", "Отчет по исследованию 2", LocalDate.of(2023, 6, 20), "Годовой отчет 2");
        doc4.setAccessDepartment("Эпидемиология");

        employeeDocumentRepository.saveAll(List.of(doc1, doc2, doc3, doc4));

        User testUser = new User();
        testUser.setUsername("user");
        testUser.setRole("USER");
        testUser.setDepartment("Эпидемиология");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(testUser));

        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setRole("USER");
        otherUser.setDepartment("Вирусология");
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));"""

code = code.replace("""        EmployeeDocument doc1 = new EmployeeDocument("EMP-999", "ORDER", "Приказ о назначении", LocalDate.of(2023, 1, 15), "Приказ №42");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-999", "REPORT", "Отчет по исследованию", LocalDate.of(2023, 6, 20), "Годовой отчет");
        doc2.setAccessDepartment("Эпидемиология");
        EmployeeDocument doc3 = new EmployeeDocument("EMP-888", "EXAM", "Экзамен", LocalDate.of(2023, 11, 10), "Оценка: отлично");
        EmployeeDocument doc4 = new EmployeeDocument("EMP-777", "Ivanov", "REPORT", "Отчет по исследованию 2", LocalDate.of(2023, 6, 20), "Годовой отчет 2");
        doc4.setAccessDepartment("Эпидемиология");

        employeeDocumentRepository.saveAll(List.of(doc1, doc2, doc3, doc4));

        User testUser = new User();
        testUser.setUsername("user");
        testUser.setRole("USER");
        testUser.setDepartment("Эпидемиология");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(testUser));

        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setRole("USER");
        otherUser.setDepartment("Вирусология");
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));""", setup_str)

import_str = """import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import java.util.Optional;
import static org.mockito.Mockito.when;"""
if import_str not in code:
    code = code.replace("import com.eneik.epidemiology.telemetry.TelemetryService;", "import com.eneik.epidemiology.telemetry.TelemetryService;\n" + import_str)

repo_str = """    @MockBean
    private UserRepository userRepository;"""
if repo_str not in code:
    code = code.replace("""    @MockBean
    private TelemetryService telemetryService;""", """    @MockBean
    private TelemetryService telemetryService;
""" + repo_str)

setup_str_find = """        EmployeeDocument doc1 = new EmployeeDocument("EMP-999", "ORDER", "Приказ о назначении", LocalDate.of(2023, 1, 15), "Приказ №42");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-999", "REPORT", "Отчет по исследованию", LocalDate.of(2023, 6, 20), "Годовой отчет");
        EmployeeDocument doc3 = new EmployeeDocument("EMP-888", "EXAM", "Экзамен", LocalDate.of(2023, 11, 10), "Оценка: отлично");
        EmployeeDocument doc4 = new EmployeeDocument("EMP-777", "Ivanov", "REPORT", "Отчет по исследованию 2", LocalDate.of(2023, 6, 20), "Годовой отчет 2");

        employeeDocumentRepository.saveAll(List.of(doc1, doc2, doc3, doc4));"""
code = code.replace(setup_str_find, setup_str)

test_auth = """    @WithMockUser(username = "other", roles = "USER")
    @Test
    @DisplayName("Given a mismatched department, when a search request is made, then restricted documents are filtered out.")
    void testSearchEmployeeDocumentsAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_id", "EMP-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Only ORDER is visible, REPORT is filtered
                .andExpect(jsonPath("$[0].title").value("Приказ о назначении"));
    }

    @WithMockUser(username = "user", roles = "USER")
    @Test
    @DisplayName("Given a matching department, when a search request is made, then restricted documents are included.")
    void testSearchEmployeeDocumentsAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_id", "EMP-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }"""
if test_auth not in code:
    code = code.replace("""    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given the API contract, when a search request is made, then the backend returns the correct document list.")""", test_auth + """\n\n    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given the API contract, when a search request is made, then the backend returns the correct document list.")""")

with open(test_file, "w") as f:
    f.write(code)
