#!/bin/bash
sed -i 's/List<EmployeeDocument> searchEmployeeDocuments(/List<EmployeeDocument> searchEmployeeDocuments(/g' src/main/java/com/eneik/epidemiology/document/EmployeeDocumentRepository.java

cat << 'JAVA_EOF' > modify.java
import java.nio.file.*;
import java.util.regex.*;

public class modify {
    public static void main(String[] args) throws Exception {
        String path = "src/main/java/com/eneik/epidemiology/document/EmployeeDocumentRepository.java";
        String content = new String(Files.readAllBytes(Paths.get(path)));
        content = content.replace("@Param(\"toDate\") java.time.LocalDate toDate\n    );", "@Param(\"toDate\") java.time.LocalDate toDate,\n            org.springframework.data.domain.Pageable pageable\n    );");
        Files.write(Paths.get(path), content.getBytes());

        path = "src/main/java/com/eneik/epidemiology/document/EmployeeDossierController.java";
        content = new String(Files.readAllBytes(Paths.get(path)));
        content = content.replace("@RequestParam(value = \"to_date\", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {", "@RequestParam(value = \"to_date\", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,\n            @RequestParam(value = \"page\", defaultValue = \"0\") int page,\n            @RequestParam(value = \"size\", defaultValue = \"100\") int size) {");
        content = content.replace("employeeId, employeeSurname, docType, scientificDirection, query, fromDate, toDate\n        );", "employeeId, employeeSurname, docType, scientificDirection, query, fromDate, toDate, org.springframework.data.domain.PageRequest.of(page, size)\n        );");
        Files.write(Paths.get(path), content.getBytes());

        path = "src/main/java/com/eneik/epidemiology/document/EmployeeDossierAnalyticsController.java";
        content = new String(Files.readAllBytes(Paths.get(path)));
        content = content.replace("employeeId, null, docType, scientificDirection, null, fromDate, toDate\n            );", "employeeId, null, docType, scientificDirection, null, fromDate, toDate, org.springframework.data.domain.Pageable.unpaged()\n            );");
        content = content.replace("employeeId, null, null, scientificDirection, null, null, null\n        );", "employeeId, null, null, scientificDirection, null, null, null, org.springframework.data.domain.Pageable.unpaged()\n        );");
        Files.write(Paths.get(path), content.getBytes());

        path = "src/test/java/com/eneik/epidemiology/document/EmployeeDocumentRepositoryTest.java";
        content = new String(Files.readAllBytes(Paths.get(path)));
        content = content.replace("employeeId, null, null, \"Вирусология\", null, null, null\n        );", "employeeId, null, null, \"Вирусология\", null, null, null, org.springframework.data.domain.Pageable.unpaged()\n        );");
        Files.write(Paths.get(path), content.getBytes());
    }
}
JAVA_EOF
javac modify.java
java modify
mvn clean test -Dtest=EmployeeDocumentRepositoryTest,EmployeeDossierControllerTest,EmployeeDossierE2ETest
