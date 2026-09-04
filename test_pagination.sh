#!/bin/bash
cat << 'INNER_EOF' > modify_repo.sh
sed -i 's/List<EmployeeDocument> searchEmployeeDocuments(/List<EmployeeDocument> searchEmployeeDocuments(/g' src/main/java/com/eneik/epidemiology/document/EmployeeDocumentRepository.java
INNER_EOF
