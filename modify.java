import java.nio.file.*;
import java.util.regex.*;

public class modify {
    public static void main(String[] args) throws Exception {
        String path = "src/main/java/com/eneik/epidemiology/document/EmployeeDossierAnalyticsController.java";
        String content = new String(Files.readAllBytes(Paths.get(path)));
        content = content.replace("List<EmployeeDocument> documents = employeeDocumentRepository.searchEmployeeDocuments(\n                employeeId, null, docType, scientificDirection, null, fromDate, toDate\n        );", "List<EmployeeDocument> documents = employeeDocumentRepository.searchEmployeeDocuments(\n                employeeId, null, docType, scientificDirection, null, fromDate, toDate, org.springframework.data.domain.Pageable.unpaged()\n        ).getContent();");
        content = content.replace("List<EmployeeDocument> documents = employeeDocumentRepository.searchEmployeeDocuments(\n                employeeId, null, null, scientificDirection, null, null, null, org.springframework.data.domain.Pageable.unpaged()\n        );", "List<EmployeeDocument> documents = employeeDocumentRepository.searchEmployeeDocuments(\n                employeeId, null, null, scientificDirection, null, null, null, org.springframework.data.domain.Pageable.unpaged()\n        ).getContent();");
        Files.write(Paths.get(path), content.getBytes());
    }
}
