package com.rit.token.ritz.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentId(String studentId);
    Optional<Student> findByEmail(String email);
    Optional<Student> findByMobileNumber(String mobileNumber);
    boolean existsByStudentId(String studentId);
    boolean existsByEmail(String email);
    boolean existsByMobileNumber(String mobileNumber);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.studentId) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "s.mobileNumber LIKE CONCAT('%', :q, '%')")
    java.util.List<Student> searchStudents(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
}
