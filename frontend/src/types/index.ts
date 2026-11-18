export type UserRole = 'STUDENT' | 'SUPER_ADMIN' | 'FINANCE_ADMIN' | 'MERCHANT_ADMIN' | 'APP_DEVELOPER';

export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string;
  role: UserRole;
  studentId?: string;
  merchantId?: string;
  applicationId?: string;
}

export interface AuthResponse {
  success: boolean;
  token: string;
  refreshToken: string;
  username: string;
  fullName: string;
  email: string;
  role: UserRole;
  studentId?: string;
  merchantId?: string;
  applicationId?: string;
  message?: string;
}

export interface Wallet {
  wallet_id: string;
  student_id: string;
  student_name: string;
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'BLOCKED' | 'CLOSED';
  last_transaction_at?: string;
  created_at: string;
}

export interface Transaction {
  id: number;
  transactionId: string;
  referenceId?: string;
  idempotencyKey?: string;
  sourceAccount: string;
  destinationAccount: string;
  amountMinorUnits: number;
  currency: string;
  transactionType: 'RECHARGE' | 'DEBIT' | 'CREDIT' | 'REFUND' | 'REVERSAL' | 'SETTLEMENT' | 'ADJUSTMENT';
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
  initiatingApplicationId?: string;
  merchantId?: string;
  studentId?: string;
  description?: string;
  previousBalanceMinorUnits?: number;
  currentBalanceMinorUnits?: number;
  failureReason?: string;
  createdAt: string;
}

export interface Merchant {
  id: number;
  merchantId: string;
  name: string;
  description?: string;
  category: 'FOOD' | 'STATIONERY' | 'TRANSPORT' | 'CAMPUS_SERVICES' | 'OTHER';
  associatedApplicationId?: string;
  contactEmail?: string;
  contactPhone?: string;
  status: string;
  settlementAccountNumber?: string;
  createdAt: string;
}

export interface MerchantAccount {
  id: number;
  merchantId: string;
  totalEarnedMinorUnits: number;
  pendingSettlementMinorUnits: number;
  settledMinorUnits: number;
  lastSettlementAt?: string;
}

export interface MerchantSettlement {
  id: number;
  settlementId: string;
  merchantId: string;
  amountMinorUnits: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  bankReference?: string;
  settlementDate?: string;
  notes?: string;
  createdAt: string;
}

export interface RegisteredApplication {
  id: number;
  applicationId: string;
  applicationName: string;
  description?: string;
  apiKey: string;
  status: string;
  rateLimitPerMinute: number;
  webhookUrl?: string;
  createdBy?: string;
  lastRequestAt?: string;
  createdAt: string;
}

export interface LedgerAccount {
  id: number;
  accountNumber: string;
  accountName: string;
  accountType: 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';
  balanceMinorUnits: number;
  currency: string;
  status: string;
}

export interface LedgerEntry {
  id: number;
  entryId: string;
  transactionId: string;
  accountNumber: string;
  entryType: 'DEBIT' | 'CREDIT';
  amountMinorUnits: number;
  balanceAfterMinorUnits: number;
  description?: string;
  createdAt: string;
}

export interface Notification {
  id: number;
  recipientId: string;
  recipientType: string;
  title: string;
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

export interface AuditLog {
  id: number;
  actorId: string;
  actorRole: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  ipAddress?: string;
  details?: string;
  createdAt: string;
}

export interface ReconciliationReport {
  totalOrdersChecked: number;
  matchedRecords: number;
  discrepanciesCount: number;
  ledgerGloballyBalanced: boolean;
  discrepancies: Array<{
    orderId: string;
    paymentId?: string;
    studentId: string;
    discrepancyType: string;
    details: string;
    detectedAt: string;
  }>;
  generatedAt: string;
}

export interface ExecutiveSummary {
  total_students: number;
  active_wallets: number;
  total_wallet_liabilities: number;
  total_recharge_volume: number;
  total_spending_volume: number;
  total_transactions: number;
  failed_transactions: number;
  pending_transactions: number;
  active_merchants: number;
  pending_settlements: number;
  ledger_balanced: boolean;
}
