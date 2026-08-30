import os
test_file = "src/test/java/com/eneik/epidemiology/document/EmployeeDossierControllerTest.java"
with open(test_file, "r") as f:
    code = f.read()

code = code.replace("""    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given the API contract, when a search request is made, then the backend returns the correct document list.")
    void testSearchEmployeeDocuments() throws Exception {""", """    @WithMockUser(username = "user", roles = "USER")
    @Test
    @DisplayName("Given the API contract, when a search request is made, then the backend returns the correct document list.")
    void testSearchEmployeeDocuments() throws Exception {""")

code = code.replace("""    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given an employee surname, when a search request is made, then the backend returns the documents associated with that surname.")
    void testSearchEmployeeDocumentsBySurname() throws Exception {""", """    @WithMockUser(username = "user", roles = "USER")
    @Test
    @DisplayName("Given an employee surname, when a search request is made, then the backend returns the documents associated with that surname.")
    void testSearchEmployeeDocumentsBySurname() throws Exception {""")

with open(test_file, "w") as f:
    f.write(code)
