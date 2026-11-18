package com.rit.token.ritz.wallet;

import com.rit.token.ritz.audit.AuditService;
import com.rit.token.ritz.ledger.AccountType;
import com.rit.token.ritz.ledger.LedgerService;
import com.rit.token.ritz.notification.NotificationService;
import com.rit.token.ritz.notification.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final StudentRepository studentRepository;
    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public WalletService(
            StudentRepository studentRepository,
            WalletRepository walletRepository,
            LedgerService ledgerService,
            NotificationService notificationService,
            AuditService auditService) {
        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.ledgerService = ledgerService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public Wallet createStudentAndWallet(String studentId, String name, String email, String mobile, String department, Integer batchYear) {
        Student student = studentRepository.findByStudentId(studentId).orElseGet(() -> {
            Student s = new Student(studentId, name, email, mobile, department, batchYear);
            return studentRepository.save(s);
        });

        return walletRepository.findByStudentId(studentId).orElseGet(() -> {
            String walletId = "WLT-RIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Wallet wallet = new Wallet(walletId, studentId);
            Wallet saved = walletRepository.save(wallet);

            // Create ledger liability account for student
            String ledgerAccountNum = "LIAB-WLT-" + studentId;
            ledgerService.getOrCreateAccount(ledgerAccountNum, "Student Wallet (" + name + ")", AccountType.LIABILITY);

            auditService.logAction("SYSTEM", "SYSTEM", "CREATE_WALLET", "WALLET", walletId, null, "Created wallet for " + studentId);
            return saved;
        });
    }

    public Optional<Wallet> getWallet(String studentId) {
        return walletRepository.findByStudentId(studentId);
    }

    public Optional<Student> getStudent(String studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    public Long getBalance(String studentId) {
        return walletRepository.findByStudentId(studentId)
                .map(Wallet::getBalanceMinorUnits)
                .orElse(0L);
    }

    @Transactional
    public Wallet suspendWallet(String studentId, String reason, String actorId) {
        Wallet wallet = walletRepository.findWithLockByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + studentId));

        wallet.setStatus(WalletStatus.SUSPENDED);
        Wallet saved = walletRepository.save(wallet);

        notificationService.sendNotification(
                studentId,
                "STUDENT",
                "Wallet Suspended",
                "Your RITZ Wallet has been suspended: " + (reason != null ? reason : "Administrative action"),
                NotificationType.WALLET_SUSPENDED
        );

        auditService.logAction(actorId, "ADMIN", "SUSPEND_WALLET", "WALLET", wallet.getWalletId(), null, "Suspended: " + reason);
        return saved;
    }

    @Transactional
    public Wallet reactivateWallet(String studentId, String actorId) {
        Wallet wallet = walletRepository.findWithLockByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + studentId));

        wallet.setStatus(WalletStatus.ACTIVE);
        Wallet saved = walletRepository.save(wallet);

        notificationService.sendNotification(
                studentId,
                "STUDENT",
                "Wallet Reactivated",
                "Your RITZ Wallet has been reactivated and is ready for campus transactions.",
                NotificationType.SYSTEM
        );

        auditService.logAction(actorId, "ADMIN", "REACTIVATE_WALLET", "WALLET", wallet.getWalletId(), null, "Reactivated wallet");
        return saved;
    }
}
