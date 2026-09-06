#!/bin/bash
sed -i 's/exactDoc.setAccessCourse("MATH-101");/exactDoc.setAccessCourse("MATH-101");\n        exactDoc.setTitle("Test Doc");/g' src/test/java/com/eneik/epidemiology/document/EmployeeDossierAuthorizationTest.java
sed -i 's/docRequiringMath1.setAccessCourse("MATH-1");/docRequiringMath1.setAccessCourse("MATH-1");\n        docRequiringMath1.setTitle("Test Doc 2");/g' src/test/java/com/eneik/epidemiology/document/EmployeeDossierAuthorizationTest.java
