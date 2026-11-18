package com.rit.token.ritz.wallet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Student Balance Query", description = "Query student balance for authorized campus applications")
public class StudentBalanceController {

    private final WalletRepository walletRepository;
    private final StudentRepository studentRepository;

    public StudentBalanceController(WalletRepository walletRepository, StudentRepository studentRepository) {
        this.walletRepository = walletRepository;
        this.studentRepository = studentRepository;
    }

    @GetMapping("/{studentId}/balance")
    @Operation(summary = "Get student wallet balance (by Student ID)")
    public ResponseEntity<Map<String, Object>> getStudentBalance(@PathVariable String studentId) {
        Wallet wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student wallet not found for ID: " + studentId));
        Student student = studentRepository.findByStudentId(studentId).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("student_id", studentId);
        response.put("student_name", student != null ? student.getName() : studentId);
        response.put("mobile_number", student != null ? student.getMobileNumber() : "");
        response.put("balance", wallet.getBalanceMinorUnits());
        response.put("currency", wallet.getCurrency());
        response.put("status", wallet.getStatus());
        response.put("can_transact", wallet.getStatus() == WalletStatus.ACTIVE);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/phone/{phoneNumber}/balance")
    @Operation(summary = "Get student wallet balance by phone number")
    public ResponseEntity<Map<String, Object>> getStudentBalanceByPhone(@PathVariable String phoneNumber) {
        String cleanedDigits = phoneNumber.replaceAll("[^0-9]", "");
        if (cleanedDigits.length() > 10) {
            cleanedDigits = cleanedDigits.substring(cleanedDigits.length() - 10);
        }

        final String phoneToFind = cleanedDigits;
        Student student = studentRepository.findByMobileNumber(phoneToFind)
                .or(() -> studentRepository.findByMobileNumber(phoneNumber))
                .orElseThrow(() -> new IllegalArgumentException("No student wallet found registered with phone number: " + phoneNumber));

        Wallet wallet = walletRepository.findByStudentId(student.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for student: " + student.getStudentId()));

        Map<String, Object> response = new HashMap<>();
        response.put("student_id", student.getStudentId());
        response.put("student_name", student.getName());
        response.put("mobile_number", student.getMobileNumber());
        response.put("balance", wallet.getBalanceMinorUnits());
        response.put("currency", wallet.getCurrency());
        response.put("status", wallet.getStatus());
        response.put("can_transact", wallet.getStatus() == WalletStatus.ACTIVE);

        return ResponseEntity.ok(response);
    }
}
