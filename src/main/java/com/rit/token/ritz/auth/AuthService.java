package com.rit.token.ritz.auth;

import com.rit.token.ritz.audit.AuditService;
import com.rit.token.ritz.auth.dto.AuthResponse;
import com.rit.token.ritz.auth.dto.LoginRequest;
import com.rit.token.ritz.auth.dto.PhoneAuthRequest;
import com.rit.token.ritz.auth.dto.RegisterRequest;
import com.rit.token.ritz.auth.dto.UserProfileResponse;
import com.rit.token.ritz.wallet.Student;
import com.rit.token.ritz.wallet.StudentRepository;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WalletService walletService;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            WalletService walletService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.walletService = walletService;
        this.auditService = auditService;
    }

    /**
     * Authenticates or automatically provisions a student user by phone number.
     * Ideal for seamless SSO / embedded project handoffs without password prompts.
     */
    @Transactional
    public AuthResponse authenticateOrRegisterByPhone(PhoneAuthRequest request) {
        String rawPhone = request.getPhoneNumber();
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        // Clean digits
        String cleanedDigits = rawPhone.replaceAll("[^0-9]", "");
        if (cleanedDigits.length() > 10) {
            cleanedDigits = cleanedDigits.substring(cleanedDigits.length() - 10);
        }

        final String phoneToFind = cleanedDigits;

        // 1. Try to find existing student by phone
        Optional<Student> studentOpt = studentRepository.findByMobileNumber(phoneToFind)
                .or(() -> studentRepository.findByMobileNumber(rawPhone));

        Student student;
        if (studentOpt.isPresent()) {
            student = studentOpt.get();
        } else {
            // Auto-provision student
            String studentId = request.getStudentId();
            if (studentId == null || studentId.trim().isEmpty()) {
                studentId = "STU" + phoneToFind;
            }

            // Ensure unique studentId
            if (studentRepository.existsByStudentId(studentId)) {
                studentId = "STU" + phoneToFind + "_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            }

            String fullName = request.getFullName();
            if (fullName == null || fullName.trim().isEmpty()) {
                fullName = "Student " + phoneToFind;
            }

            String email = request.getEmail();
            if (email == null || email.trim().isEmpty()) {
                email = phoneToFind + "@campus.rit.edu.in";
            }
            if (studentRepository.existsByEmail(email)) {
                email = phoneToFind + "." + UUID.randomUUID().toString().substring(0, 4) + "@campus.rit.edu.in";
            }

            walletService.createStudentAndWallet(
                    studentId,
                    fullName,
                    email,
                    phoneToFind,
                    "General",
                    2026
            );

            student = studentRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new IllegalStateException("Failed to create student for phone: " + phoneToFind));
            log.info("Auto-provisioned student: {} with phone: {}", student.getStudentId(), phoneToFind);
        }

        // 2. Ensure corresponding User record exists for JWT auth
        final String sid = student.getStudentId();
        Student finalStudent = student;
        User user = userRepository.findByStudentId(sid)
                .or(() -> userRepository.findByUsername(sid))
                .or(() -> userRepository.findByUsername(finalStudent.getMobileNumber()))
                .orElseGet(() -> {
                    String username = finalStudent.getMobileNumber();
                    if (userRepository.existsByUsername(username)) {
                        username = finalStudent.getStudentId();
                    }
                    if (userRepository.existsByUsername(username)) {
                        username = "user_" + finalStudent.getMobileNumber() + "_" + UUID.randomUUID().toString().substring(0, 4);
                    }

                    String userEmail = finalStudent.getEmail();
                    if (userRepository.existsByEmail(userEmail)) {
                        userEmail = "u." + finalStudent.getMobileNumber() + "." + UUID.randomUUID().toString().substring(0, 4) + "@campus.rit.edu.in";
                    }

                    User newUser = new User(
                            username,
                            passwordEncoder.encode(UUID.randomUUID().toString()),
                            finalStudent.getName(),
                            userEmail,
                            Role.STUDENT
                    );
                    newUser.setStudentId(finalStudent.getStudentId());
                    User saved = userRepository.save(newUser);
                    log.info("Created user account for student: {}", finalStudent.getStudentId());
                    return saved;
                });

        if (!user.isActive()) {
            throw new IllegalStateException("Account is suspended. Please contact administrator.");
        }

        String jwt = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        auditService.logAction(user.getUsername(), "STUDENT", "PHONE_SSO_LOGIN", "USER", user.getId().toString(), null, "User authenticated via phone SSO: " + phoneToFind);

        return AuthResponse.success(
                jwt,
                refreshToken,
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStudentId(),
                user.getMerchantId(),
                user.getApplicationId()
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.STUDENT;

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getEmail(),
                role
        );

        if (role == Role.STUDENT) {
            String studentId = request.getStudentId() != null ? request.getStudentId() : request.getUsername().toUpperCase();
            user.setStudentId(studentId);
            // Create student record and digital wallet automatically
            walletService.createStudentAndWallet(
                    studentId,
                    request.getFullName(),
                    request.getEmail(),
                    request.getMobileNumber() != null ? request.getMobileNumber() : "9876543210",
                    request.getDepartment() != null ? request.getDepartment() : "Computer Science",
                    request.getBatchYear() != null ? request.getBatchYear() : 2026
            );
        } else if (role == Role.MERCHANT_ADMIN) {
            user.setMerchantId(request.getMerchantId());
        } else if (role == Role.APP_DEVELOPER) {
            user.setApplicationId(request.getApplicationId());
        }

        User savedUser = userRepository.save(user);

        String jwt = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        auditService.logAction(savedUser.getUsername(), savedUser.getRole().name(), "USER_REGISTERED", "USER", savedUser.getId().toString(), null, "User registered: " + savedUser.getUsername());

        return AuthResponse.success(
                jwt,
                refreshToken,
                savedUser.getUsername(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getStudentId(),
                savedUser.getMerchantId(),
                savedUser.getApplicationId()
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .or(() -> userRepository.findByStudentId(request.getUsername()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.logAction(request.getUsername(), "ANONYMOUS", "LOGIN_FAILED", "USER", null, null, "Incorrect password attempt");
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new IllegalStateException("Account is suspended. Please contact administrator.");
        }

        String jwt = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        auditService.logAction(user.getUsername(), user.getRole().name(), "USER_LOGIN_SUCCESS", "USER", user.getId().toString(), null, "User logged in");

        return AuthResponse.success(
                jwt,
                refreshToken,
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStudentId(),
                user.getMerchantId(),
                user.getApplicationId()
        );
    }

    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        UserProfileResponse res = new UserProfileResponse();
        res.setId(user.getId());
        res.setUsername(user.getUsername());
        res.setFullName(user.getFullName());
        res.setEmail(user.getEmail());
        res.setRole(user.getRole());
        res.setStudentId(user.getStudentId());
        res.setMerchantId(user.getMerchantId());
        res.setApplicationId(user.getApplicationId());
        res.setActive(user.isActive());
        return res;
    }
}
