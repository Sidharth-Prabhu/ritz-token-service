package com.rit.token.ritz.report;

import com.rit.token.ritz.ledger.LedgerEntry;
import com.rit.token.ritz.ledger.LedgerEntryRepository;
import com.rit.token.ritz.merchant.MerchantSettlement;
import com.rit.token.ritz.merchant.MerchantSettlementRepository;
import com.rit.token.ritz.payment.RechargeOrder;
import com.rit.token.ritz.payment.RechargeOrderRepository;
import com.rit.token.ritz.refund.RefundRequest;
import com.rit.token.ritz.refund.RefundRepository;
import com.rit.token.ritz.transaction.TransactionStatus;
import com.rit.token.ritz.transaction.TransactionType;
import com.rit.token.ritz.transaction.WalletTransaction;
import com.rit.token.ritz.transaction.WalletTransactionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.List;

@Service
public class ReportService {

    private final RechargeOrderRepository rechargeOrderRepository;
    private final WalletTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final RefundRepository refundRepository;
    private final MerchantSettlementRepository settlementRepository;

    public ReportService(
            RechargeOrderRepository rechargeOrderRepository,
            WalletTransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            RefundRepository refundRepository,
            MerchantSettlementRepository settlementRepository) {
        this.rechargeOrderRepository = rechargeOrderRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.refundRepository = refundRepository;
        this.settlementRepository = settlementRepository;
    }

    public String generateRechargeReportCsv() {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("Order ID", "Student ID", "Amount INR", "Token Amount", "Status", "Gateway", "Payment ID", "Date").build())) {
            List<RechargeOrder> orders = rechargeOrderRepository.findAll();
            for (RechargeOrder o : orders) {
                printer.printRecord(
                        o.getOrderId(),
                        o.getStudentId(),
                        o.getAmountRupees(),
                        o.getTokenAmount(),
                        o.getStatus(),
                        o.getPaymentGateway(),
                        o.getPaymentId() != null ? o.getPaymentId() : "N/A",
                        o.getCreatedAt()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating recharge CSV", e);
        }
        return sw.toString();
    }

    public String generateTransactionReportCsv() {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("Transaction ID", "Reference ID", "Student ID", "Merchant ID", "Type", "Amount", "Status", "Prev Balance", "Curr Balance", "Date").build())) {
            List<WalletTransaction> txns = transactionRepository.findAll();
            for (WalletTransaction t : txns) {
                printer.printRecord(
                        t.getTransactionId(),
                        t.getReferenceId(),
                        t.getStudentId(),
                        t.getMerchantId() != null ? t.getMerchantId() : "N/A",
                        t.getTransactionType(),
                        t.getAmountMinorUnits(),
                        t.getStatus(),
                        t.getPreviousBalanceMinorUnits(),
                        t.getCurrentBalanceMinorUnits(),
                        t.getCreatedAt()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating transactions CSV", e);
        }
        return sw.toString();
    }

    public String generateLedgerReportCsv() {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("Entry ID", "Transaction ID", "Account Number", "Type", "Amount", "Balance After", "Description", "Date").build())) {
            List<LedgerEntry> entries = ledgerEntryRepository.findAll();
            for (LedgerEntry e : entries) {
                printer.printRecord(
                        e.getEntryId(),
                        e.getTransactionId(),
                        e.getAccountNumber(),
                        e.getEntryType(),
                        e.getAmountMinorUnits(),
                        e.getBalanceAfterMinorUnits(),
                        e.getDescription(),
                        e.getCreatedAt()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating ledger CSV", e);
        }
        return sw.toString();
    }

    public String generateRefundReportCsv() {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("Refund ID", "Original Txn ID", "Student ID", "Merchant ID", "Amount", "Reason", "Status", "Approved By", "Date").build())) {
            List<RefundRequest> refunds = refundRepository.findAll();
            for (RefundRequest r : refunds) {
                printer.printRecord(
                        r.getRefundId(),
                        r.getOriginalTransactionId(),
                        r.getStudentId(),
                        r.getMerchantId() != null ? r.getMerchantId() : "N/A",
                        r.getAmountMinorUnits(),
                        r.getReason(),
                        r.getStatus(),
                        r.getApprovedBy() != null ? r.getApprovedBy() : "N/A",
                        r.getCreatedAt()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating refund CSV", e);
        }
        return sw.toString();
    }

    public String generateSettlementReportCsv() {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("Settlement ID", "Merchant ID", "Amount", "Status", "Bank Reference", "Settlement Date", "Notes").build())) {
            List<MerchantSettlement> settlements = settlementRepository.findAll();
            for (MerchantSettlement s : settlements) {
                printer.printRecord(
                        s.getSettlementId(),
                        s.getMerchantId(),
                        s.getAmountMinorUnits(),
                        s.getStatus(),
                        s.getBankReference() != null ? s.getBankReference() : "N/A",
                        s.getSettlementDate() != null ? s.getSettlementDate() : "N/A",
                        s.getNotes() != null ? s.getNotes() : ""
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating settlements CSV", e);
        }
        return sw.toString();
    }
}
