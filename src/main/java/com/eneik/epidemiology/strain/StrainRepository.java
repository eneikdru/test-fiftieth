package com.eneik.epidemiology.strain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StrainRepository extends JpaRepository<Strain, UUID> {

    @Query("SELECT s FROM Strain s WHERE " +
           "(:isAdmin = true OR (s.accessDepartment IS NULL AND s.accessCourse IS NULL) OR s.accessDepartment = :userDepartment OR (s.accessCourse IN :userCoursesList))")
    List<Strain> findAccessibleStrains(@Param("isAdmin") boolean isAdmin,
                                       @Param("userDepartment") String userDepartment,
                                       @Param("userCoursesList") List<String> userCoursesList);
}
