#!/bin/bash

# Modify EmployeeDossierController
awk '
BEGIN { in_method = 0 }
/public ResponseEntity<\?> searchEmployeeDocuments/ { in_method = 1 }
in_method && /@RequestParam\(value = "to_date", required = false\)/ {
    print $0
    print "            @RequestParam(value = \"page\", defaultValue = \"0\") int page,"
    print "            @RequestParam(value = \"size\", defaultValue = \"100\") int size) {"
    next
}
in_method && /@RequestParam\(value = "to_date", required = false\) @DateTimeFormat\(iso = DateTimeFormat\.ISO\.DATE\) LocalDate toDate\) \{/ {
    # Skip original line
    next
}
in_method && /List<EmployeeDocument> documents = employeeDocumentRepository.searchEmployeeDocuments\(/ {
    print "        List<EmployeeDocument> documents = employeeDocumentRepository.searchEmployeeDocuments("
    print "                employeeId, employeeSurname, docType, scientificDirection, query, fromDate, toDate"
    print "        );"
    print ""
    print "        if (currentUser != null && !\"ADMIN\".equals(currentUser.getRole())) {"
    print "            documents = documents.stream().filter(d -> {"
    print "                if (!\"STRAIN_ISOLATION\".equals(d.getDocType()) && !\"REPORT\".equals(d.getDocType())) return true;"
    print "                if (d.getAccessDepartment() == null && d.getAccessCourse() == null) return true;"
    print "                boolean depMatch = d.getAccessDepartment() != null && d.getAccessDepartment().equals(currentUser.getDepartment());"
    print "                boolean courseMatch = d.getAccessCourse() != null && currentUser.getCourses() != null && currentUser.getCourses().contains(d.getAccessCourse());"
    print "                return depMatch || courseMatch;"
    print "            }).toList();"
    print "        }"
    print ""
    print "        int start = Math.min((int) org.springframework.data.domain.PageRequest.of(page, size).getOffset(), documents.size());"
    print "        int end = Math.min((start + size), documents.size());"
    print "        org.springframework.data.domain.Page<EmployeeDocument> pagedResult = new org.springframework.data.domain.PageImpl<>(documents.subList(start, end), org.springframework.data.domain.PageRequest.of(page, size), documents.size());"
    print ""
    print "        return ResponseEntity.ok(pagedResult);"
    print "    }"
    in_method = 0

    # We need to skip the rest of the original method block
    getline
    while ( !/    @PostMapping\("\/reports"\)/ ) {
        getline
    }
    print $0
    next
}
{ print }
' src/main/java/com/eneik/epidemiology/document/EmployeeDossierController.java > tmp.java
mv tmp.java src/main/java/com/eneik/epidemiology/document/EmployeeDossierController.java
