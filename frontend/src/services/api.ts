const BASE_URL = '';

export class ApiError extends Error {
  status: number;
  data: any;

  constructor(status: number, message: string, data?: any) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('ritz_dev_token') || localStorage.getItem('ritz_token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('content-type');
  let data: any;
  if (contentType && contentType.includes('application/json')) {
    data = await response.json();
  } else if (contentType && (contentType.includes('text/csv') || contentType.includes('application/octet-stream'))) {
    return (await response.blob()) as unknown as T;
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    const errorMsg = data && data.error ? data.error : (data && data.message ? data.message : `HTTP ${response.status}`);
    throw new ApiError(response.status, errorMsg, data);
  }

  return data as T;
}

export const api = {
  // Authentication
  login: (data: any) => request<any>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify(data) }),
  loginByPhone: (data: { phone_number: string; full_name?: string; email?: string; student_id?: string }) =>
    request<any>('/api/v1/auth/phone', { method: 'POST', body: JSON.stringify(data) }),
  register: (data: any) => request<any>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(data) }),
  getProfile: () => request<any>('/api/v1/auth/me'),

  // Student Wallet
  getWallet: () => request<any>('/api/v1/wallet'),
  getWalletPublic: (phone?: string, studentId?: string) =>
    request<any>(`/api/v1/wallet/public?${phone ? `phone=${encodeURIComponent(phone)}&` : ''}${studentId ? `student_id=${encodeURIComponent(studentId)}` : ''}`),
  getWalletTransactions: (page = 0, size = 20) => request<any>(`/api/v1/wallet/transactions?page=${page}&size=${size}`),
  getTransaction: (id: string) => request<any>(`/api/v1/wallet/transactions/${id}`),
  initiateRecharge: (amount: number, phone?: string, studentId?: string) =>
    request<any>('/api/v1/wallet/recharge', { method: 'POST', body: JSON.stringify({ amount, phone, student_id: studentId }) }),
  getPaymentConfig: () => request<any>('/api/v1/payments/config'),
  verifyPayment: (orderId: string, paymentId: string, signature: string) =>
    request<any>('/api/v1/payments/verify', {
      method: 'POST',
      body: JSON.stringify({ order_id: orderId, payment_id: paymentId, signature })
    }),
  confirmSandboxPayment: (orderId: string, paymentId?: string, signature?: string) =>
    request<any>('/api/v1/payments/sandbox/confirm', {
      method: 'POST',
      body: JSON.stringify({ order_id: orderId, payment_id: paymentId, signature })
    }),

  // Notifications
  getNotifications: () => request<any>('/api/v1/notifications'),
  markNotificationRead: (id: number) => request<any>(`/api/v1/notifications/${id}/read`, { method: 'PATCH' }),
  markAllNotificationsRead: () => request<any>('/api/v1/notifications/read-all', { method: 'POST' }),

  // Refunds
  requestRefund: (transactionId: string, amount: number, reason: string) =>
    request<any>('/api/v1/refunds/request', { method: 'POST', body: JSON.stringify({ transaction_id: transactionId, amount, reason }) }),
  getRefunds: (page = 0, size = 20) => request<any>(`/api/v1/refunds?page=${page}&size=${size}`),
  reviewRefund: (refundId: string, approve: boolean) =>
    request<any>(`/api/v1/refunds/${refundId}/review`, { method: 'POST', body: JSON.stringify({ approve }) }),

  // Admin & Financial Reports
  getExecutiveSummary: () => request<any>('/api/v1/reports/summary'),
  getLedgerAccounts: () => request<any>('/api/v1/ledger/accounts'),
  getLedgerEntries: (page = 0, size = 50, accountNumber?: string) =>
    request<any>(`/api/v1/ledger/entries?page=${page}&size=${size}${accountNumber ? `&accountNumber=${accountNumber}` : ''}`),
  verifyLedgerBalance: () => request<any>('/api/v1/ledger/verify'),
  getReconciliation: () => request<any>('/api/v1/reconciliation/check'),
  getAuditLogs: (page = 0, size = 50) => request<any>(`/api/v1/audit/logs?page=${page}&size=${size}`),
  getAllStudents: () => request<any>('/api/v1/students'),
  getAllWallets: () => request<any>('/api/v1/wallets'),

  // Wallet Management
  suspendWallet: (studentId: string, reason: string) =>
    request<any>(`/api/v1/wallets/${studentId}/suspend`, { method: 'POST', body: JSON.stringify({ reason }) }),
  reactivateWallet: (studentId: string) =>
    request<any>(`/api/v1/wallets/${studentId}/reactivate`, { method: 'POST' }),

  // Merchants
  getMerchants: () => request<any>('/api/v1/merchants'),
  getMerchantDetails: (id: string) => request<any>(`/api/v1/merchants/${id}`),
  getMerchantTransactions: (id: string, page = 0, size = 20) =>
    request<any>(`/api/v1/merchants/${id}/transactions?page=${page}&size=${size}`),
  getMerchantSettlements: (id: string) => request<any>(`/api/v1/merchants/${id}/settlements`),
  requestSettlement: (merchantId: string, amount: number, notes?: string) =>
    request<any>(`/api/v1/merchants/${merchantId}/settlements`, { method: 'POST', body: JSON.stringify({ amount, notes }) }),
  completeSettlement: (settlementId: string, bankReference: string) =>
    request<any>(`/api/v1/merchants/settlements/${settlementId}/complete`, { method: 'POST', body: JSON.stringify({ bank_reference: bankReference }) }),

  // Applications (Developer Portal)
  loginDevPortal: (password: string) => request<any>('/api/v1/dev-portal/login', { method: 'POST', body: JSON.stringify({ password }) }),
  getApplications: () => request<any>('/api/v1/applications'),
  getApplication: (id: string) => request<any>(`/api/v1/applications/${id}`),
  registerApplication: (data: any) => request<any>('/api/v1/applications', { method: 'POST', body: JSON.stringify(data) }),
  rotateCredentials: (id: string) => request<any>(`/api/v1/applications/${id}/rotate-credentials`, { method: 'POST' }),
  revokeCredentials: (id: string) => request<any>(`/api/v1/applications/${id}/revoke-credentials`, { method: 'POST' }),

  // Application Direct APIs (Sandbox Runner)
  testDebit: (data: any, apiKey: string, idempotencyKey?: string) =>
    request<any>('/api/v1/transactions/debit', {
      method: 'POST',
      headers: {
        'X-API-KEY': apiKey,
        ...(idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {})
      },
      body: JSON.stringify(data)
    }),

  testValidateBalance: (studentId: string, amount: number, apiKey: string) =>
    request<any>('/api/v1/transactions/validate', {
      method: 'POST',
      headers: { 'X-API-KEY': apiKey },
      body: JSON.stringify({ student_id: studentId, amount })
    }),

  testStudentBalanceQuery: (studentId: string, apiKey: string) =>
    request<any>(`/api/v1/students/${studentId}/balance`, {
      method: 'GET',
      headers: { 'X-API-KEY': apiKey }
    }),

  // CSV Exports
  exportCsvUrl: (type: 'recharges' | 'transactions' | 'ledger' | 'refunds' | 'settlements') =>
    `/api/v1/reports/${type}/csv`
};
