package com.eneik.epidemiology.strain;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
}
