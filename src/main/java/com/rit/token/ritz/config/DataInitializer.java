package com.rit.token.ritz.config;

import com.rit.token.ritz.application.ApplicationService;
import com.rit.token.ritz.application.PermissionScope;
import com.rit.token.ritz.application.RegisteredApplicationRepository;
import com.rit.token.ritz.auth.Role;
import com.rit.token.ritz.auth.User;
import com.rit.token.ritz.auth.UserRepository;
import com.rit.token.ritz.ledger.AccountType;
import com.rit.token.ritz.ledger.LedgerAccountRepository;
import com.rit.token.ritz.ledger.LedgerService;
import com.rit.token.ritz.merchant.MerchantCategory;
import com.rit.token.ritz.merchant.MerchantRepository;
import com.rit.token.ritz.merchant.MerchantService;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletRepository;
import com.rit.token.ritz.wallet.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final MerchantService merchantService;
    private final MerchantRepository merchantRepository;
    private final ApplicationService applicationService;
    private final RegisteredApplicationRepository applicationRepository;
    private final LedgerService ledgerService;
    private final LedgerAccountRepository accountRepository;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            WalletService walletService,
            WalletRepository walletRepository,
            MerchantService merchantService,
            MerchantRepository merchantRepository,
            ApplicationService applicationService,
            RegisteredApplicationRepository applicationRepository,
            LedgerService ledgerService,
            LedgerAccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.merchantService = merchantService;
        this.merchantRepository = merchantRepository;
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
        this.ledgerService = ledgerService;
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing RITZ Wallet base ecosystem...");

        // 1. Base Ledger Accounts
        ledgerService.getOrCreateAccount("ASSET-RAZORPAY-CLEARING", "Razorpay Payment Clearing", AccountType.ASSET);
        ledgerService.getOrCreateAccount("ASSET-SANDBOX-CLEARING", "Sandbox Payment Clearing", AccountType.ASSET);
        ledgerService.getOrCreateAccount("ASSET-BANK-SETTLEMENT", "Bank Settlement Clearing", AccountType.ASSET);

        // 2. Default Administrative Users
        if (!userRepository.existsByUsername("superadmin")) {
            User superAdmin = new User("superadmin", passwordEncoder.encode("RitzAdmin2026!"), "Super Administrator", "admin@ritchennai.edu.in", Role.SUPER_ADMIN);
            userRepository.save(superAdmin);
            log.info("Created default SUPER_ADMIN user: superadmin");
        }

        if (!userRepository.existsByUsername("financeadmin")) {
            User financeAdmin = new User("financeadmin", passwordEncoder.encode("RitzFinance2026!"), "Finance Controller", "finance@ritchennai.edu.in", Role.FINANCE_ADMIN);
            userRepository.save(financeAdmin);
            log.info("Created default FINANCE_ADMIN user: financeadmin");
        }

        if (!userRepository.existsByUsername("tilloadmin")) {
            User merchantUser = new User("tilloadmin", passwordEncoder.encode("TilloPass2026!"), "Tillo Canteen Manager", "canteen@ritchennai.edu.in", Role.MERCHANT_ADMIN);
            merchantUser.setMerchantId("TILLO");
            userRepository.save(merchantUser);
            log.info("Created default MERCHANT_ADMIN user: tilloadmin");
        }

        if (!userRepository.existsByUsername("developer")) {
            User devUser = new User("developer", passwordEncoder.encode("RitzDev2026!"), "Platform Developer", "dev@ritchennai.edu.in", Role.APP_DEVELOPER);
            devUser.setApplicationId("TILLO");
            userRepository.save(devUser);
            log.info("Created default APP_DEVELOPER user: developer");
        }

        // 3. Register Merchants
        if (!merchantRepository.existsByMerchantId("TILLO")) {
            merchantService.registerMerchant(
                    "TILLO",
                    "Tillo Campus Canteen",
                    "Central Food Court and Cafeteria Service",
                    MerchantCategory.FOOD,
                    "TILLO",
                    "canteen@ritchennai.edu.in",
                    "+91 98765 43210"
            );
            log.info("Registered TILLO merchant");
        }

        if (!merchantRepository.existsByMerchantId("STATIONERY_STORE")) {
            merchantService.registerMerchant(
                    "STATIONERY_STORE",
                    "RIT Stationery & Book Depot",
                    "Campus Academic Supplies and Printing",
                    MerchantCategory.STATIONERY,
                    "STATIONERY",
                    "stationery@ritchennai.edu.in",
                    "+91 98765 43211"
            );
            log.info("Registered STATIONERY_STORE merchant");
        }

        if (!merchantRepository.existsByMerchantId("TRANSPORT_DEPT")) {
            merchantService.registerMerchant(
                    "TRANSPORT_DEPT",
                    "RIT Campus Bus & Transport",
                    "Student Transportation Passes & Shuttle",
                    MerchantCategory.TRANSPORT,
                    "TRANSPORT",
                    "transport@ritchennai.edu.in",
                    "+91 98765 43212"
            );
            log.info("Registered TRANSPORT_DEPT merchant");
        }

        if (!merchantRepository.existsByMerchantId("SMARTPRINT")) {
            merchantService.registerMerchant(
                    "SMARTPRINT",
                    "Smartprint Campus Printing & Copy Center",
                    "Campus Automated Cloud & Kiosk Printing Services",
                    MerchantCategory.STATIONERY,
                    "SMARTPRINT",
                    "smartprint@ritchennai.edu.in",
                    "+91 98765 43213"
            );
            log.info("Registered SMARTPRINT merchant");
        }

        // 4. Register Applications & Generate API Credentials
        if (!applicationRepository.existsByApplicationId("TILLO")) {
            applicationService.registerApplication(
                    "TILLO",
                    "Tillo Point-of-Sale System",
                    "Campus canteen POS and token deduction service",
                    List.of(PermissionScope.TRANSACTION_DEBIT, PermissionScope.TRANSACTION_CREDIT, PermissionScope.TRANSACTION_REFUND, PermissionScope.TRANSACTION_VALIDATE, PermissionScope.BALANCE_READ),
                    "SYSTEM"
            );
            log.info("Registered application: TILLO");
        }

        if (!applicationRepository.existsByApplicationId("SMARTPRINT")) {
            applicationService.registerApplication(
                    "SMARTPRINT",
                    "Smartprint Printing Station",
                    "Campus Automated Cloud and Kiosk Printing Services",
                    List.of(PermissionScope.TRANSACTION_DEBIT, PermissionScope.TRANSACTION_CREDIT, PermissionScope.TRANSACTION_REFUND, PermissionScope.TRANSACTION_VALIDATE, PermissionScope.BALANCE_READ),
                    "SYSTEM"
            );
            log.info("Registered application: SMARTPRINT");
        }

        if (!applicationRepository.existsByApplicationId("CAMPUS_ERP")) {
            applicationService.registerApplication(
                    "CAMPUS_ERP",
                    "Rajalakshmi ERP Portal",
                    "Central student academic ERP integration",
                    List.of(PermissionScope.BALANCE_READ, PermissionScope.TRANSACTION_VALIDATE),
                    "SYSTEM"
            );
            log.info("Registered application: CAMPUS_ERP");
        }

        // 5. Sample Student & Initial Wallet Balance (matches prompt test case: RIT2026001, initial balance 600)
        if (!userRepository.existsByUsername("RIT2026001")) {
            User studentUser = new User("RIT2026001", passwordEncoder.encode("StudentPass2026!"), "Aditya Sharma", "rit2026001@ritchennai.edu.in", Role.STUDENT);
            studentUser.setStudentId("RIT2026001");
            userRepository.save(studentUser);

            Wallet wallet = walletService.createStudentAndWallet(
                    "RIT2026001",
                    "Aditya Sharma",
                    "rit2026001@ritchennai.edu.in",
                    "9876543210",
                    "Computer Science & Engineering",
                    2026
            );

            // Seed initial wallet balance of 600 tokens with balanced double-entry
            // DEBIT: ASSET-SANDBOX-CLEARING 600
            // CREDIT: LIAB-WLT-RIT2026001 600
            long initialBalance = 600L;
            wallet.credit(initialBalance);
            walletRepository.save(wallet);

            ledgerService.recordDoubleEntry(
                    "RZTXN-INIT-100001",
                    "ASSET-SANDBOX-CLEARING",
                    "LIAB-WLT-RIT2026001",
                    initialBalance,
                    "Initial student wallet seed balance"
            );

            log.info("Created test student RIT2026001 with wallet balance 600 RITZ tokens");
        }

        log.info("RITZ Wallet ecosystem initialization completed successfully.");
    }
}
