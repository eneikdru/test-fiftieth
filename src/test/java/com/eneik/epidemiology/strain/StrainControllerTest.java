package com.eneik.epidemiology.strain;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StrainControllerTest {

    @Mock
    private StrainRepository strainRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private StrainController strainController;

    @BeforeEach
    void setUp() {
        strainController = new StrainController(strainRepository, userRepository);
    }

    @Test
    @DisplayName("Given default page and size parameters, When getStrains is called, Then returns paginated list with X-Total-Count and X-Total-Pages headers")
    void testGetStrains_DefaultPagination() {
        User user = new User();
        user.setUsername("researcher1");
        user.setRole("RESEARCHER");
        user.setDepartment("BIO");
        user.setCourses("BIO101,BIO102");

        when(authentication.getName()).thenReturn("researcher1");
        when(userRepository.findByUsername("researcher1")).thenReturn(Optional.of(user));

        Strain strain1 = new Strain();
        strain1.setId(UUID.randomUUID());
        strain1.setName("Strain A");

        Page<Strain> mockPage = new PageImpl<>(List.of(strain1), PageRequest.of(0, 20), 100);
        when(strainRepository.findAccessibleStrains(eq(false), eq("BIO"), eq(List.of("BIO101", "BIO102")), any(Pageable.class)))
                .thenReturn(mockPage);

        ResponseEntity<List<Strain>> response = strainController.getStrains(authentication, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("100", response.getHeaders().getFirst("X-Total-Count"));
        assertEquals("5", response.getHeaders().getFirst("X-Total-Pages"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(strainRepository, times(1)).findAccessibleStrains(eq(false), eq("BIO"), eq(List.of("BIO101", "BIO102")), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
    }

    @Test
    @DisplayName("Given custom page and size parameters, When getStrains is called, Then passes expected Pageable to repository")
    void testGetStrains_CustomPagination() {
        User adminUser = new User();
        adminUser.setUsername("admin1");
        adminUser.setRole("ADMIN");

        when(authentication.getName()).thenReturn("admin1");
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));

        Strain strain1 = new Strain();
        strain1.setId(UUID.randomUUID());
        strain1.setName("Strain B1");

        Strain strain2 = new Strain();
        strain2.setId(UUID.randomUUID());
        strain2.setName("Strain B2");

        Page<Strain> mockPage = new PageImpl<>(List.of(strain1, strain2), PageRequest.of(2, 5), 12);
        when(strainRepository.findAccessibleStrains(eq(true), any(), eq(Collections.emptyList()), any(Pageable.class)))
                .thenReturn(mockPage);

        ResponseEntity<List<Strain>> response = strainController.getStrains(authentication, 2, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("12", response.getHeaders().getFirst("X-Total-Count"));
        assertEquals("3", response.getHeaders().getFirst("X-Total-Pages"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(strainRepository, times(1)).findAccessibleStrains(eq(true), any(), eq(Collections.emptyList()), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
    }

    @Test
    @DisplayName("Given unauthenticated or non-existent user, When getStrains is called, Then returns 401 Unauthorized")
    void testGetStrains_Unauthenticated() {
        ResponseEntity<List<Strain>> responseNullAuth = strainController.getStrains(null, 0, 20);
        assertEquals(HttpStatus.UNAUTHORIZED, responseNullAuth.getStatusCode());

        when(authentication.getName()).thenReturn("unknownUser");
        when(userRepository.findByUsername("unknownUser")).thenReturn(Optional.empty());

        ResponseEntity<List<Strain>> responseUnknownUser = strainController.getStrains(authentication, 0, 20);
        assertEquals(HttpStatus.UNAUTHORIZED, responseUnknownUser.getStatusCode());

        verify(strainRepository, never()).findAccessibleStrains(anyBoolean(), any(), any(), any());
    }

    @Test
    @DisplayName("Given valid strain request, When createStrain is called, Then saves strain and returns 201 Created")
    void testCreateStrain_Success() {
        User user = new User();
        user.setUsername("researcher1");
        user.setRole("RESEARCHER");

        when(authentication.getName()).thenReturn("researcher1");
        when(userRepository.findByUsername("researcher1")).thenReturn(Optional.of(user));

        StrainRequestDto request = new StrainRequestDto();
        request.setName("New Strain");
        request.setDescription("Test Description");
        request.setIdentifiedDate(LocalDate.of(2024, 5, 10));
        request.setOriginCountry("Russia");
        request.setSeverityLevel("MEDIUM");
        request.setAccessDepartment("BIO");
        request.setAccessCourse("BIO101");

        when(strainRepository.save(any(Strain.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = strainController.createStrain(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody() instanceof Strain);

        Strain created = (Strain) response.getBody();
        assertEquals("New Strain", created.getName());
        assertEquals("BIO", created.getAccessDepartment());
        assertEquals("BIO101", created.getAccessCourse());
    }

    @Test
    @DisplayName("Given invalid strain request with empty name, When createStrain is called, Then returns 400 Bad Request")
    void testCreateStrain_Invalid() {
        User user = new User();
        user.setUsername("researcher1");

        when(authentication.getName()).thenReturn("researcher1");
        when(userRepository.findByUsername("researcher1")).thenReturn(Optional.of(user));

        StrainRequestDto request = new StrainRequestDto();
        request.setName("   ");

        ResponseEntity<?> response = strainController.createStrain(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(strainRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given existing strain, When updateStrain is called, Then updates strain details and access rules")
    void testUpdateStrain_Success() {
        User user = new User();
        user.setUsername("admin1");

        when(authentication.getName()).thenReturn("admin1");
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(user));

        UUID strainId = UUID.randomUUID();
        Strain existing = new Strain();
        existing.setId(strainId);
        existing.setName("Old Name");

        when(strainRepository.findById(strainId)).thenReturn(Optional.of(existing));
        when(strainRepository.save(any(Strain.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StrainRequestDto request = new StrainRequestDto();
        request.setName("Updated Name");
        request.setAccessDepartment("CHEM");

        ResponseEntity<?> response = strainController.updateStrain(authentication, strainId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Strain updated = (Strain) response.getBody();
        assertNotNull(updated);
        assertEquals("Updated Name", updated.getName());
        assertEquals("CHEM", updated.getAccessDepartment());
    }

    @Test
    @DisplayName("Given existing strain, When deleteStrain is called, Then removes strain and returns 204 No Content")
    void testDeleteStrain_Success() {
        User user = new User();
        user.setUsername("admin1");

        when(authentication.getName()).thenReturn("admin1");
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(user));

        UUID strainId = UUID.randomUUID();
        when(strainRepository.existsById(strainId)).thenReturn(true);

        ResponseEntity<Void> response = strainController.deleteStrain(authentication, strainId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(strainRepository, times(1)).deleteById(strainId);
    }
}
