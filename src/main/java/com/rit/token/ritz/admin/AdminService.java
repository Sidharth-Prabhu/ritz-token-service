package com.rit.token.ritz.admin;

import com.rit.token.ritz.admin.dto.AdminDepositRequest;
import com.rit.token.ritz.admin.dto.AdminDepositResponse;
import com.rit.token.ritz.admin.dto.AdminStatsResponse;
import com.rit.token.ritz.admin.dto.AdminUserDto;
import com.rit.token.ritz.admin.dto.StudentSearchResult;
import com.rit.token.ritz.audit.AuditService;
import com.rit.token.ritz.auth.Role;
import com.rit.token.ritz.auth.User;
import com.rit.token.ritz.auth.UserRepository;
import com.rit.token.ritz.ledger.AccountType;
import com.rit.token.ritz.ledger.LedgerEntryRepository;
import com.rit.token.ritz.ledger.LedgerService;
import com.rit.token.ritz.merchant.Merchant;
import com.rit.token.ritz.merchant.MerchantAccountRepository;
import com.rit.token.ritz.merchant.MerchantRepository;
import com.rit.token.ritz.notification.NotificationService;
import com.rit.token.ritz.notification.NotificationType;
import com.rit.token.ritz.transaction.TransactionStatus;
import com.rit.token.ritz.transaction.TransactionType;
import com.rit.token.ritz.transaction.WalletTransaction;
import com.rit.token.ritz.transaction.WalletTransactionRepository;
import com.rit.token.ritz.wallet.Student;
import com.rit.token.ritz.wallet.StudentRepository;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletRepository;
import com.rit.token.ritz.wallet.WalletService;
import com.rit.token.ritz.wallet.WalletStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final WalletRepository walletRepository;
    private final StudentRepository studentRepository;
    private final WalletTransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantAccountRepository merchantAccountRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final WalletService walletService;
    private final UserRepository userRepository;

    public AdminService(
            WalletRepository walletRepository,
            StudentRepository studentRepository,
            WalletTransactionRepository transactionRepository,
            LedgerService ledgerService,
            LedgerEntryRepository ledgerEntryRepository,
            MerchantRepository merchantRepository,
            MerchantAccountRepository merchantAccountRepository,
            NotificationService notificationService,
            AuditService auditService,
            WalletService walletService,
            UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.studentRepository = studentRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.merchantRepository = merchantRepository;
        this.merchantAccountRepository = merchantAccountRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.walletService = walletService;
        this.userRepository = userRepository;
    }

    /**
     * Aggregates live circulation and ecosystem stats.
     */
    public AdminStatsResponse getCirculationStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        Long totalCirculation = walletRepository.getTotalCirculationMinorUnits();
        stats.setTotalCirculation(totalCirculation != null ? totalCirculation : 0L);

        Long totalIssued = transactionRepository.getTotalIssuedVolume();
        stats.setTotalIssued(totalIssued != null ? totalIssued : 0L);

        Long totalSpent = transactionRepository.getTotalSpendingVolume();
        stats.setTotalSpent(totalSpent != null ? totalSpent : 0L);

        Long totalRefunded = transactionRepository.getTotalRefundVolume();
        stats.setTotalRefunded(totalRefunded != null ? totalRefunded : 0L);

        stats.setTotalStudents(studentRepository.count());
        stats.setActiveWallets(walletRepository.countByStatus(WalletStatus.ACTIVE));
        stats.setTotalTransactions(transactionRepository.count());
        stats.setFailedTransactions(transactionRepository.countByStatus(TransactionStatus.FAILED));
        stats.setPendingTransactions(transactionRepository.countByStatus(TransactionStatus.PENDING));
        stats.setActiveMerchants(merchantRepository.count());

        Long pendingSettlements = merchantAccountRepository.getTotalPendingSettlements();
        stats.setPendingSettlements(pendingSettlements != null ? pendingSettlements : 0L);

        Long totalDebits = ledgerEntryRepository.getTotalDebitVolume();
        Long totalCredits = ledgerEntryRepository.getTotalCreditVolume();
        stats.setLedgerTotalDebits(totalDebits != null ? totalDebits : 0L);
        stats.setLedgerTotalCredits(totalCredits != null ? totalCredits : 0L);
        stats.setLedgerBalanced(totalDebits != null && totalDebits.equals(totalCredits));

        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        stats.setVolumeLast24h(transactionRepository.getTotalVolumeSince(last24h));
        stats.setIssuedLast24h(transactionRepository.getIssuedVolumeSince(last24h));

        // Top wallets (return up to 50)
        List<Wallet> topWalletsList = walletRepository.findTopWalletsByBalance(PageRequest.of(0, 50));
        List<Map<String, Object>> topWalletsData = new ArrayList<>();
        for (Wallet w : topWalletsList) {
            Map<String, Object> map = new HashMap<>();
            map.put("student_id", w.getStudentId());
            map.put("balance", w.getBalanceMinorUnits());
            map.put("status", w.getStatus());
            studentRepository.findByStudentId(w.getStudentId()).ifPresent(s -> {
                map.put("student_name", s.getName());
                map.put("mobile_number", s.getMobileNumber());
            });
            if (!map.containsKey("mobile_number") && w.getStudentId() != null && w.getStudentId().matches("^STU\\d{10}$")) {
                map.put("mobile_number", w.getStudentId().substring(3));
            }
            topWalletsData.add(map);
        }
        stats.setTopWallets(topWalletsData);

        // Merchant breakdown
        List<Merchant> merchants = merchantRepository.findAll();
        List<Map<String, Object>> merchantList = new ArrayList<>();
        for (Merchant m : merchants) {
            Map<String, Object> map = new HashMap<>();
            map.put("merchant_id", m.getMerchantId());
            map.put("business_name", m.getName());
            map.put("category", m.getCategory());
            map.put("status", m.getStatus());
            Long vol = transactionRepository.getMerchantTotalVolume(m.getMerchantId());
            map.put("total_volume", vol != null ? vol : 0L);
            merchantList.add(map);
        }
        stats.setMerchantSummary(merchantList);

        return stats;
    }

    /**
     * Retrieves filtered and paginated recent transactions.
     */
    public Page<WalletTransaction> getRecentTransactions(
            int page,
            int limit,
            TransactionType type,
            TransactionStatus status,
            String search) {
        int boundedLimit = Math.min(Math.max(limit, 1), 200);
        return transactionRepository.findFiltered(
                type,
                status,
                search != null && !search.isBlank() ? search.trim() : null,
                PageRequest.of(page, boundedLimit)
        );
    }

    /**
     * Executes atomic direct deposit with double-entry ledger bookkeeping.
     */
    @Transactional
    public AdminDepositResponse executeDirectDeposit(AdminDepositRequest request, String adminActor) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly greater than 0");
        }

        // 1. Resolve student ID
        String effectiveStudentId = request.getStudentId();
        String phoneNumber = request.getPhoneNumber();

        if ((effectiveStudentId == null || effectiveStudentId.isBlank()) && (phoneNumber != null && !phoneNumber.isBlank())) {
            String cleaned = phoneNumber.replaceAll("[^0-9]", "");
            if (cleaned.length() > 10) cleaned = cleaned.substring(cleaned.length() - 10);
            final String phoneToFind = cleaned;

            Optional<Student> studentOpt = studentRepository.findByMobileNumber(phoneToFind)
                    .or(() -> studentRepository.findByMobileNumber(phoneNumber));

            if (studentOpt.isPresent()) {
                effectiveStudentId = studentOpt.get().getStudentId();
            } else {
                // Auto create student and wallet
                Wallet created = walletService.createStudentAndWallet(
                        "STU" + phoneToFind,
                        "Student " + phoneToFind,
                        phoneToFind + "@campus.rit.edu.in",
                        phoneToFind,
                        "Campus General",
                        2026
                );
                effectiveStudentId = created.getStudentId();
            }
        }

        if (effectiveStudentId == null || effectiveStudentId.isBlank()) {
            throw new IllegalArgumentException("Either student_id or phone_number must be specified");
        }

        final String targetStudentId = effectiveStudentId;

        // Ensure wallet exists or create
        Wallet wallet = walletRepository.findWithLockByStudentId(targetStudentId)
                .orElseGet(() -> {
                    Student s = studentRepository.findByStudentId(targetStudentId)
                            .orElseGet(() -> {
                                Student newS = new Student(
                                        targetStudentId,
                                        "Student " + targetStudentId,
                                        targetStudentId.toLowerCase() + "@campus.rit.edu.in",
                                        "9876543210",
                                        "General",
                                        2026
                                );
                                return studentRepository.save(newS);
                            });
                    Wallet w = new Wallet("WLT-" + targetStudentId, targetStudentId);
                    return walletRepository.save(w);
                });

        long previousBalance = wallet.getBalanceMinorUnits();
        long tokensToCredit = request.getAmount();

        // 2. Credit student wallet
        wallet.credit(tokensToCredit);
        walletRepository.save(wallet);
        long newBalance = wallet.getBalanceMinorUnits();

        // 3. Double-entry accounting
        String category = request.getCategory() != null && !request.getCategory().isBlank()
                ? request.getCategory().toUpperCase()
                : "MANUAL_RECHARGE";

        String sourceAccount = switch (category) {
            case "SCHOLARSHIP", "MERIT_GRANT" -> "EQUITY-CAMPUS-SCHOLARSHIP";
            case "CAMPUS_SUBSIDY" -> "EQUITY-CAMPUS-SUBSIDY";
            case "EVENT_PRIZE" -> "EQUITY-EVENT-PRIZES";
            case "CASH_DESK_RECHARGE" -> "ASSET-CASH-DESK";
            default -> "ASSET-ADMIN-TREASURY";
        };
        String destinationAccount = "LIAB-WLT-" + targetStudentId;

        ledgerService.getOrCreateAccount(sourceAccount, "Treasury Allocation (" + category + ")", AccountType.ASSET);
        ledgerService.getOrCreateAccount(destinationAccount, "Student Wallet (" + targetStudentId + ")", AccountType.LIABILITY);

        String voucherId = "VCH-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        ledgerService.recordDoubleEntry(
                voucherId,
                sourceAccount,
                destinationAccount,
                tokensToCredit,
                "Admin Direct Deposit [" + category + "]: " + (request.getNotes() != null ? request.getNotes() : "Institutional Grant")
        );

        // 4. Record WalletTransaction
        String txnId = "TXN-DEP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String refId = request.getReferenceId() != null && !request.getReferenceId().isBlank()
                ? request.getReferenceId()
                : "DEP-" + System.currentTimeMillis();

        WalletTransaction txn = new WalletTransaction();
        txn.setTransactionId(txnId);
        txn.setReferenceId(refId);
        txn.setSourceAccount(sourceAccount);
        txn.setDestinationAccount(destinationAccount);
        txn.setAmountMinorUnits(tokensToCredit);
        txn.setTransactionType(TransactionType.CREDIT);
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setStudentId(targetStudentId);
        txn.setInitiatingApplicationId("ADMIN_PORTAL");
        txn.setMerchantId("ADMIN_TREASURY");
        txn.setDescription("Direct Deposit (" + category + "): " + (request.getNotes() != null ? request.getNotes() : "Admin allocation"));
        txn.setPreviousBalanceMinorUnits(previousBalance);
        txn.setCurrentBalanceMinorUnits(newBalance);
        transactionRepository.save(txn);

        // 5. In-app student notification
        notificationService.sendNotification(
                targetStudentId,
                "STUDENT",
                "Direct Token Deposit: +" + tokensToCredit + " RITZ",
                "Your wallet has been credited with " + tokensToCredit + " RITZ tokens (" + category + "). " +
                        (request.getNotes() != null ? "Note: " + request.getNotes() : ""),
                NotificationType.RECHARGE_SUCCESS
        );

        // 6. Audit log
        auditService.logAction(
                adminActor != null ? adminActor : "ADMIN",
                "SUPER_ADMIN",
                "DIRECT_DEPOSIT",
                "WALLET",
                wallet.getWalletId(),
                null,
                "Deposited " + tokensToCredit + " tokens to " + targetStudentId + " (" + category + ")"
        );

        // Student metadata
        Optional<Student> studentOpt = studentRepository.findByStudentId(targetStudentId);
        String studentName = studentOpt.map(Student::getName).orElse(targetStudentId);
        String resolvedPhone = studentOpt.map(Student::getMobileNumber).orElse(phoneNumber != null ? phoneNumber : "N/A");

        log.info("Direct deposit successful: Student={} Amount={} Category={} NewBal={} by {}",
                targetStudentId, tokensToCredit, category, newBalance, adminActor);

        return AdminDepositResponse.success(
                txnId,
                targetStudentId,
                studentName,
                resolvedPhone,
                tokensToCredit,
                previousBalance,
                newBalance,
                category,
                refId,
                voucherId,
                adminActor,
                request.getNotes()
        );
    }

    /**
     * Search students for auto-complete in direct deposit.
     */
    public List<StudentSearchResult> searchStudents(String query, int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 50);
        if (query == null || query.isBlank()) {
            List<Student> recentStudents = studentRepository.findAll(PageRequest.of(0, boundedLimit, Sort.by("id").descending())).getContent();
            return enrichStudentsWithWallet(recentStudents);
        }
        List<Student> students = studentRepository.searchStudents(query.trim(), PageRequest.of(0, boundedLimit));
        return enrichStudentsWithWallet(students);
    }

    private List<StudentSearchResult> enrichStudentsWithWallet(List<Student> students) {
        List<StudentSearchResult> results = new ArrayList<>();
        for (Student s : students) {
            Optional<Wallet> w = walletRepository.findByStudentId(s.getStudentId());
            Long bal = w.map(Wallet::getBalanceMinorUnits).orElse(0L);
            String status = w.map(wallet -> wallet.getStatus().name()).orElse("ACTIVE");
            results.add(new StudentSearchResult(
                    s.getStudentId(),
                    s.getName(),
                    s.getMobileNumber(),
                    null, // Do not expose dummy emails
                    s.getDepartment(),
                    bal,
                    status
            ));
        }
        return results;
    }

    /**
     * Returns unified list of all users and students across the campus with wallet balances.
     */
    public Map<String, Object> getAllUsers(int page, int limit, String search, String roleFilter) {
        int boundedLimit = Math.min(Math.max(limit, 1), 250);
        int pageNum = Math.max(page, 0);

        List<User> allUsers = userRepository.findAll(Sort.by("id").ascending());
        List<Student> allStudents = studentRepository.findAll();
        Map<String, Student> studentMapByStudentId = new HashMap<>();
        Map<String, Student> studentMapByPhone = new HashMap<>();
        for (Student s : allStudents) {
            if (s.getStudentId() != null) studentMapByStudentId.put(s.getStudentId(), s);
            if (s.getMobileNumber() != null) studentMapByPhone.put(s.getMobileNumber(), s);
        }

        List<Wallet> allWallets = walletRepository.findAll();
        Map<String, Wallet> walletByStudentId = new HashMap<>();
        for (Wallet w : allWallets) {
            walletByStudentId.put(w.getStudentId(), w);
        }

        List<AdminUserDto> dtos = new ArrayList<>();
        Set<String> processedStudentIds = new HashSet<>();

        for (User u : allUsers) {
            Student s = null;
            if (u.getStudentId() != null) {
                s = studentMapByStudentId.get(u.getStudentId());
                processedStudentIds.add(u.getStudentId());
            } else if (u.getUsername() != null && studentMapByPhone.containsKey(u.getUsername())) {
                s = studentMapByPhone.get(u.getUsername());
                processedStudentIds.add(s.getStudentId());
            }

            String studentId = u.getStudentId() != null ? u.getStudentId() : (s != null ? s.getStudentId() : null);
            String fullName = (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : (s != null ? s.getName() : u.getUsername());
            String mobile = s != null ? s.getMobileNumber() : (u.getUsername() != null && u.getUsername().matches("^\\d{10}$") ? u.getUsername() : (studentId != null && studentId.matches("^STU\\d{10}$") ? studentId.substring(3) : null));
            String email = u.getEmail() != null ? u.getEmail() : (s != null ? s.getEmail() : null);
            if (email != null && (email.contains("@campus.rit.edu.in") || email.matches("^\\d{10}@.*"))) {
                email = null; // Strip generated dummy email
            }
            String roleStr = u.getRole() != null ? u.getRole().name() : "STUDENT";

            Wallet w = studentId != null ? walletByStudentId.get(studentId) : null;
            String walletId = w != null ? w.getWalletId() : null;
            Long balance = w != null ? w.getBalanceMinorUnits() : 0L;
            String walletStatus = w != null ? w.getStatus().name() : (u.isActive() ? "ACTIVE" : "INACTIVE");

            dtos.add(new AdminUserDto(
                    u.getId(),
                    u.getUsername(),
                    studentId,
                    fullName,
                    mobile,
                    email,
                    roleStr,
                    walletId,
                    balance,
                    walletStatus,
                    u.getCreatedAt()
            ));
        }

        for (Student s : allStudents) {
            if (!processedStudentIds.contains(s.getStudentId())) {
                Wallet w = walletByStudentId.get(s.getStudentId());
                String walletId = w != null ? w.getWalletId() : null;
                Long balance = w != null ? w.getBalanceMinorUnits() : 0L;
                String walletStatus = w != null ? w.getStatus().name() : "ACTIVE";
                String email = s.getEmail();
                if (email != null && (email.contains("@campus.rit.edu.in") || email.matches("^\\d{10}@.*"))) {
                    email = null;
                }

                dtos.add(new AdminUserDto(
                        null,
                        s.getMobileNumber(),
                        s.getStudentId(),
                        s.getName(),
                        s.getMobileNumber(),
                        email,
                        "STUDENT",
                        walletId,
                        balance,
                        walletStatus,
                        s.getCreatedAt()
                ));
            }
        }

        if (roleFilter != null && !roleFilter.isBlank() && !roleFilter.equalsIgnoreCase("ALL")) {
            String targetRole = roleFilter.trim().toUpperCase();
            dtos.removeIf(dto -> dto.getRole() == null || !dto.getRole().equalsIgnoreCase(targetRole));
        }

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            dtos.removeIf(dto -> {
                boolean matchName = dto.getFullName() != null && dto.getFullName().toLowerCase().contains(q);
                boolean matchUsername = dto.getUsername() != null && dto.getUsername().toLowerCase().contains(q);
                boolean matchStudentId = dto.getStudentId() != null && dto.getStudentId().toLowerCase().contains(q);
                boolean matchMobile = dto.getMobileNumber() != null && dto.getMobileNumber().contains(q);
                boolean matchEmail = dto.getEmail() != null && dto.getEmail().toLowerCase().contains(q);
                boolean matchWalletId = dto.getWalletId() != null && dto.getWalletId().toLowerCase().contains(q);
                return !matchName && !matchUsername && !matchStudentId && !matchMobile && !matchEmail && !matchWalletId;
            });
        }

        int totalElements = dtos.size();
        int totalPages = (int) Math.ceil((double) totalElements / boundedLimit);
        int fromIndex = Math.min(pageNum * boundedLimit, totalElements);
        int toIndex = Math.min(fromIndex + boundedLimit, totalElements);
        List<AdminUserDto> pageContent = dtos.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("content", pageContent);
        result.put("total_elements", totalElements);
        result.put("total_pages", totalPages);
        result.put("current_page", pageNum);
        result.put("limit", boundedLimit);

        return result;
    }
}
