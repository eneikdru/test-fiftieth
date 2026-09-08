package com.eneik.epidemiology.strain;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/strains")
public class StrainController {

    private final StrainRepository strainRepository;
    private final UserRepository userRepository;

    public StrainController(StrainRepository strainRepository, UserRepository userRepository) {
        this.strainRepository = strainRepository;
        this.userRepository = userRepository;
    }

    private boolean hasAccess(User user, Strain strain) {
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        boolean isPublic = strain.getAccessDepartment() == null && strain.getAccessCourse() == null;
        boolean matchesDepartment = strain.getAccessDepartment() != null && strain.getAccessDepartment().equals(user.getDepartment());
        boolean matchesCourse = false;
        if (strain.getAccessCourse() != null && user.getCourses() != null) {
            List<String> userCourses = Arrays.stream(user.getCourses().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            matchesCourse = userCourses.contains(strain.getAccessCourse());
        }

        return isPublic || matchesDepartment || matchesCourse;
    }

    @GetMapping
    public ResponseEntity<List<Strain>> getStrains(
            Authentication authentication,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        boolean isAdmin = "ADMIN".equals(user.getRole());
        String department = user.getDepartment();

        List<String> coursesList = Collections.emptyList();
        if (user.getCourses() != null && !user.getCourses().trim().isEmpty()) {
            coursesList = Arrays.stream(user.getCourses().split(","))
                                .map(String::trim)
                                .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Strain> strainsPage = strainRepository.findAccessibleStrains(isAdmin, department, coursesList, pageable);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(strainsPage.getTotalElements()));
        headers.add("X-Total-Pages", String.valueOf(strainsPage.getTotalPages()));

        return ResponseEntity.ok().headers(headers).body(strainsPage.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Strain> getStrainById(
            Authentication authentication,
            @PathVariable UUID id) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Strain strain = strainRepository.findById(id).orElse(null);
        if (strain == null) {
            return ResponseEntity.status(404).build();
        }

        if (!hasAccess(user, strain)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(strain);
    }

    @PostMapping
    public ResponseEntity<?> createStrain(
            Authentication authentication,
            @RequestBody StrainRequestDto request) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.status(400).body("Название штамма обязательно.");
        }

        Strain strain = new Strain();
        strain.setId(UUID.randomUUID());
        strain.setName(request.getName().trim());
        strain.setDescription(request.getDescription());
        strain.setIdentifiedDate(request.getIdentifiedDate());
        strain.setOriginCountry(request.getOriginCountry());
        strain.setSeverityLevel(request.getSeverityLevel());
        strain.setAccessDepartment(request.getAccessDepartment());
        strain.setAccessCourse(request.getAccessCourse());

        Strain savedStrain = strainRepository.save(strain);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedStrain);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStrain(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody StrainRequestDto request) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Strain strain = strainRepository.findById(id).orElse(null);
        if (strain == null) {
            return ResponseEntity.status(404).build();
        }

        if (!hasAccess(user, strain)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.status(400).body("Название штамма обязательно.");
        }

        strain.setName(request.getName().trim());
        strain.setDescription(request.getDescription());
        strain.setIdentifiedDate(request.getIdentifiedDate());
        strain.setOriginCountry(request.getOriginCountry());
        strain.setSeverityLevel(request.getSeverityLevel());
        strain.setAccessDepartment(request.getAccessDepartment());
        strain.setAccessCourse(request.getAccessCourse());

        Strain updatedStrain = strainRepository.save(strain);

        return ResponseEntity.ok(updatedStrain);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStrain(
            Authentication authentication,
            @PathVariable UUID id) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Strain strain = strainRepository.findById(id).orElse(null);
        if (strain == null) {
            return ResponseEntity.status(404).build();
        }

        if (!hasAccess(user, strain)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        strainRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
