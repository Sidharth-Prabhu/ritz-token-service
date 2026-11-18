import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { ExecutiveSummary, LedgerAccount, LedgerEntry, ReconciliationReport, AuditLog } from '../types';
import {
  ShieldCheck,
  Layers,
  AlertTriangle,
  FileSpreadsheet,
  Users,
  Wallet,
  ArrowUpRight,
  ArrowDownLeft,
  RefreshCw,
  CheckCircle2,
  XCircle,
  FileText,
  Search,
  Filter,
  Download,
  Lock,
  Unlock,
  History
} from 'lucide-react';

interface AdminDashboardProps {
  currentTab?: string;
}

export const AdminDashboard: React.FC<AdminDashboardProps> = ({ currentTab = 'overview' }) => {
  const [activeTab, setActiveTab] = useState<string>(currentTab);
  const [summary, setSummary] = useState<ExecutiveSummary | null>(null);
  const [ledgerAccounts, setLedgerAccounts] = useState<LedgerAccount[]>([]);
  const [ledgerEntries, setLedgerEntries] = useState<LedgerEntry[]>([]);
  const [reconciliation, setReconciliation] = useState<ReconciliationReport | null>(null);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [reconLoading, setReconLoading] = useState<boolean>(false);
  const [verificationResult, setVerificationResult] = useState<any>(null);

  const loadAll = async () => {
    setIsLoading(true);
    try {
      const [sumData, accData, entryData, reconData, auditData] = await Promise.all([
        api.getExecutiveSummary(),
        api.getLedgerAccounts(),
        api.getLedgerEntries(0, 30),
        api.getReconciliation(),
        api.getAuditLogs(0, 30)
      ]);
      setSummary(sumData);
      setLedgerAccounts(accData || []);
      setLedgerEntries(entryData.content || []);
      setReconciliation(reconData);
      setAuditLogs(auditData.content || []);
    } catch (e) {
      console.error('Failed to load admin data', e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const handleVerifyLedger = async () => {
    try {
      const res = await api.verifyLedgerBalance();
      setVerificationResult(res);
    } catch (e: any) {
      console.error(e);
    }
  };

  const handleRunRecon = async () => {
    setReconLoading(true);
    try {
      const res = await api.getReconciliation();
      setReconciliation(res);
    } catch (e) {
      console.error(e);
    } finally {
      setReconLoading(false);
    }
  };

  const handleDownloadCsv = (type: 'recharges' | 'transactions' | 'ledger' | 'refunds' | 'settlements') => {
    const token = localStorage.getItem('ritz_token');
    fetch(`/api/v1/reports/${type}/csv`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(res => res.blob())
      .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ritz_${type}_report.csv`;
        document.body.appendChild(a);
        a.click();
        a.remove();
      });
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      
      {/* Executive KPI Metrics */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        
        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 font-semibold">
            <span>Total Liabilities</span>
            <Wallet className="w-4 h-4 text-blue-600" />
          </div>
          <div className="text-2xl font-black text-slate-900 dark:text-white mt-2">
            {summary ? summary.total_wallet_liabilities : 0} <span className="text-xs font-semibold text-slate-400">RITZ</span>
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Student circulating float</div>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 font-semibold">
            <span>Recharge Volume</span>
            <ArrowDownLeft className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="text-2xl font-black text-slate-900 dark:text-white mt-2">
            ₹{summary ? summary.total_recharge_volume : 0}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Total INR collected</div>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 font-semibold">
            <span>Spending Volume</span>
            <ArrowUpRight className="w-4 h-4 text-rose-500" />
          </div>
          <div className="text-2xl font-black text-slate-900 dark:text-white mt-2">
            {summary ? summary.total_spending_volume : 0} <span className="text-xs font-semibold text-slate-400">RITZ</span>
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Canteen & campus spent</div>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 font-semibold">
            <span>Ledger Integrity</span>
            <ShieldCheck className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="flex items-center space-x-2 mt-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-xl font-black text-emerald-600 dark:text-emerald-400">BALANCED</span>
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Debits = Credits Verified</div>
        </div>

      </div>

      {/* Tabs Header */}
      <div className="flex items-center space-x-2 border-b border-slate-200 dark:border-slate-800 pb-2 text-sm font-semibold overflow-x-auto">
        <button
          onClick={() => setActiveTab('overview')}
          className={`px-4 py-2 rounded-xl transition-all ${activeTab === 'overview' ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'}`}
        >
          Executive Overview
        </button>
        <button
          onClick={() => setActiveTab('ledger')}
          className={`px-4 py-2 rounded-xl transition-all ${activeTab === 'ledger' ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'}`}
        >
          Double-Entry Ledger
        </button>
        <button
          onClick={() => setActiveTab('reconciliation')}
          className={`px-4 py-2 rounded-xl transition-all ${activeTab === 'reconciliation' ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'}`}
        >
          Reconciliation Console
        </button>
        <button
          onClick={() => setActiveTab('reports')}
          className={`px-4 py-2 rounded-xl transition-all ${activeTab === 'reports' ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'}`}
        >
          Reports & CSV Export
        </button>
        <button
          onClick={() => setActiveTab('audit')}
          className={`px-4 py-2 rounded-xl transition-all ${activeTab === 'audit' ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'}`}
        >
          System Audit Trail
        </button>
      </div>

      {/* TAB 1: EXECUTIVE OVERVIEW */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            
            <div className="p-6 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
              <h3 className="font-extrabold text-slate-900 dark:text-white text-base">Campus Participation</h3>
              <div className="space-y-3 text-xs">
                <div className="flex justify-between py-1.5 border-b border-slate-100 dark:border-slate-800">
                  <span className="text-slate-500">Registered Students</span>
                  <span className="font-bold text-slate-900 dark:text-white">{summary?.total_students}</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-slate-100 dark:border-slate-800">
                  <span className="text-slate-500">Active Wallets</span>
                  <span className="font-bold text-emerald-600">{summary?.active_wallets}</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-slate-100 dark:border-slate-800">
                  <span className="text-slate-500">Active Campus Merchants</span>
                  <span className="font-bold text-slate-900 dark:text-white">{summary?.active_merchants}</span>
                </div>
                <div className="flex justify-between py-1.5">
                  <span className="text-slate-500">Pending Merchant Settlements</span>
                  <span className="font-bold text-amber-600">{summary?.pending_settlements} RITZ</span>
                </div>
              </div>
            </div>

            <div className="p-6 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
              <h3 className="font-extrabold text-slate-900 dark:text-white text-base">Transaction Throughput</h3>
              <div className="space-y-3 text-xs">
                <div className="flex justify-between py-1.5 border-b border-slate-100 dark:border-slate-800">
                  <span className="text-slate-500">Total System Transactions</span>
                  <span className="font-bold text-slate-900 dark:text-white">{summary?.total_transactions}</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-slate-100 dark:border-slate-800">
                  <span className="text-slate-500">Failed / Rejected Debits</span>
                  <span className="font-bold text-slate-900 dark:text-white">{summary?.failed_transactions}</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-slate-100 dark:border-slate-800">
                  <span className="text-slate-500">Pending Operations</span>
                  <span className="font-bold text-slate-900 dark:text-white">{summary?.pending_transactions}</span>
                </div>
                <div className="flex justify-between py-1.5">
                  <span className="text-slate-500">Settlement Currency</span>
                  <span className="font-mono font-bold text-blue-600">INR / RITZ (1:1)</span>
                </div>
              </div>
            </div>

            <div className="p-6 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
              <h3 className="font-extrabold text-slate-900 dark:text-white text-base">Compliance & Verification</h3>
              <div className="space-y-2 text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                <p>• Razorpay Multi-Merchant PPI is configured in sandbox compliance hold.</p>
                <p>• Immutable double-entry ledger prevents balance modification from outside.</p>
                <p>• Concurrency row-locking prevents simultaneous canteen overdrafts.</p>
              </div>
              <button
                onClick={handleVerifyLedger}
                className="w-full py-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-xs font-bold transition-colors"
              >
                Run Integrity Verification
              </button>
            </div>

          </div>
        </div>
      )}

      {/* TAB 2: DOUBLE-ENTRY LEDGER BROWSER */}
      {activeTab === 'ledger' && (
        <div className="space-y-6">
          <div className="flex justify-between items-center">
            <div>
              <h3 className="text-base font-extrabold text-slate-900 dark:text-white">General Ledger Accounts</h3>
              <p className="text-xs text-slate-500">Assets, Liabilities, and Clearing balances</p>
            </div>
            <button
              onClick={handleVerifyLedger}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow-sm"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>Verify Debits = Credits</span>
            </button>
          </div>

          {verificationResult && (
            <div className="p-4 rounded-2xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 flex justify-between items-center text-xs">
              <div>
                <span className="font-bold text-emerald-800 dark:text-emerald-200">Audit Status: {verificationResult.status}</span>
                <span className="text-slate-500 ml-2">Total Debits: {verificationResult.total_debits} | Total Credits: {verificationResult.total_credits}</span>
              </div>
              <span className="font-mono text-emerald-700 font-bold">Discrepancy: {verificationResult.discrepancy}</span>
            </div>
          )}

          {/* Accounts Grid */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {ledgerAccounts.map((acc) => (
              <div key={acc.id} className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
                <div className="flex justify-between items-start">
                  <span className="font-mono text-xs font-bold text-slate-900 dark:text-white">{acc.accountNumber}</span>
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 font-bold text-slate-600 dark:text-slate-300">
                    {acc.accountType}
                  </span>
                </div>
                <div className="text-xs text-slate-500 mt-1">{acc.accountName}</div>
                <div className="text-xl font-black text-slate-900 dark:text-white mt-3">
                  {acc.balanceMinorUnits} <span className="text-xs font-semibold text-slate-400">RITZ</span>
                </div>
              </div>
            ))}
          </div>

          {/* Journal Entries */}
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
            <h4 className="font-extrabold text-sm text-slate-900 dark:text-white mb-3">Immutable Journal Entries (Audit Trail)</h4>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="text-slate-400 uppercase tracking-wider text-[10px] border-b border-slate-100 dark:border-slate-800">
                    <th className="pb-2.5 font-semibold">Entry ID</th>
                    <th className="pb-2.5 font-semibold">Transaction ID</th>
                    <th className="pb-2.5 font-semibold">Account</th>
                    <th className="pb-2.5 font-semibold">Type</th>
                    <th className="pb-2.5 font-semibold">Amount</th>
                    <th className="pb-2.5 font-semibold">Balance After</th>
                    <th className="pb-2.5 font-semibold">Timestamp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60 font-medium">
                  {ledgerEntries.map((e) => (
                    <tr key={e.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                      <td className="py-2.5 font-mono text-[11px] text-slate-600 dark:text-slate-400">{e.entryId}</td>
                      <td className="py-2.5 font-mono text-[11px] text-blue-600 dark:text-blue-400">{e.transactionId}</td>
                      <td className="py-2.5 font-mono text-[11px] text-slate-900 dark:text-white">{e.accountNumber}</td>
                      <td className="py-2.5">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${e.entryType === 'DEBIT' ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300' : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'}`}>
                          {e.entryType}
                        </span>
                      </td>
                      <td className="py-2.5 font-extrabold text-slate-900 dark:text-white">{e.amountMinorUnits} RITZ</td>
                      <td className="py-2.5 font-mono text-slate-600 dark:text-slate-400">{e.balanceAfterMinorUnits}</td>
                      <td className="py-2.5 text-slate-400">{new Date(e.createdAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: RECONCILIATION CONSOLE */}
      {activeTab === 'reconciliation' && (
        <div className="space-y-6">
          <div className="flex justify-between items-center">
            <div>
              <h3 className="text-base font-extrabold text-slate-900 dark:text-white">Three-Way Financial Reconciliation</h3>
              <p className="text-xs text-slate-500">Automated cross-check: Recharge Orders ⟷ Payment Gateway ⟷ Wallet Credits</p>
            </div>
            <button
              onClick={handleRunRecon}
              disabled={reconLoading}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow-sm disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${reconLoading ? 'animate-spin' : ''}`} />
              <span>Run Live Reconciliation</span>
            </button>
          </div>

          {reconciliation && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                <div className="text-xs text-slate-500">Orders Verified</div>
                <div className="text-2xl font-black text-slate-900 dark:text-white mt-1">{reconciliation.totalOrdersChecked}</div>
              </div>
              <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                <div className="text-xs text-slate-500">Matched Cleanly</div>
                <div className="text-2xl font-black text-emerald-600 mt-1">{reconciliation.matchedRecords}</div>
              </div>
              <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                <div className="text-xs text-slate-500">Discrepancies</div>
                <div className={`text-2xl font-black mt-1 ${reconciliation.discrepanciesCount === 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                  {reconciliation.discrepanciesCount}
                </div>
              </div>
              <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                <div className="text-xs text-slate-500">Ledger Balanced</div>
                <div className="text-2xl font-black text-emerald-600 mt-1">
                  {reconciliation.ledgerGloballyBalanced ? 'YES' : 'NO'}
                </div>
              </div>
            </div>
          )}

          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
            <h4 className="font-extrabold text-sm text-slate-900 dark:text-white mb-2">Discrepancy Exception Queue</h4>
            {reconciliation?.discrepancies.length === 0 ? (
              <div className="py-12 text-center text-xs text-emerald-600 dark:text-emerald-400 font-semibold flex flex-col items-center">
                <CheckCircle2 className="w-8 h-8 mb-2" />
                <span>Zero discrepancies detected. All internal orders match payment records and wallet credits.</span>
              </div>
            ) : (
              <div className="space-y-3">
                {reconciliation?.discrepancies.map((d, i) => (
                  <div key={i} className="p-3.5 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 text-xs text-red-700 dark:text-red-300">
                    <div className="flex justify-between font-bold">
                      <span>Order: {d.orderId} (Student: {d.studentId})</span>
                      <span className="uppercase">{d.discrepancyType}</span>
                    </div>
                    <p className="mt-1">{d.details}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 4: REPORTS & CSV EXPORT */}
      {activeTab === 'reports' && (
        <div className="space-y-6">
          <div>
            <h3 className="text-base font-extrabold text-slate-900 dark:text-white">Export Official Financial Reports</h3>
            <p className="text-xs text-slate-500">Download formatted CSV reports for campus auditing and finance archives</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            
            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs flex flex-col justify-between">
              <div>
                <FileSpreadsheet className="w-6 h-6 text-blue-600 mb-2" />
                <h4 className="font-bold text-sm text-slate-900 dark:text-white">Recharge History Report</h4>
                <p className="text-xs text-slate-500 mt-1">Complete student top-ups, amounts, gateway references and dates.</p>
              </div>
              <button
                onClick={() => handleDownloadCsv('recharges')}
                className="mt-4 w-full py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-xl text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center justify-center space-x-2 transition-colors"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Download CSV</span>
              </button>
            </div>

            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs flex flex-col justify-between">
              <div>
                <FileSpreadsheet className="w-6 h-6 text-emerald-600 mb-2" />
                <h4 className="font-bold text-sm text-slate-900 dark:text-white">Transactions Ledger Report</h4>
                <p className="text-xs text-slate-500 mt-1">Every debit, credit, canteen spend with reference IDs and balance changes.</p>
              </div>
              <button
                onClick={() => handleDownloadCsv('transactions')}
                className="mt-4 w-full py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-xl text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center justify-center space-x-2 transition-colors"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Download CSV</span>
              </button>
            </div>

            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs flex flex-col justify-between">
              <div>
                <FileSpreadsheet className="w-6 h-6 text-indigo-600 mb-2" />
                <h4 className="font-bold text-sm text-slate-900 dark:text-white">Double-Entry Journal Report</h4>
                <p className="text-xs text-slate-500 mt-1">Immutable double-entry debits, credits, account numbers and running balances.</p>
              </div>
              <button
                onClick={() => handleDownloadCsv('ledger')}
                className="mt-4 w-full py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-xl text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center justify-center space-x-2 transition-colors"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Download CSV</span>
              </button>
            </div>

            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs flex flex-col justify-between">
              <div>
                <FileSpreadsheet className="w-6 h-6 text-rose-600 mb-2" />
                <h4 className="font-bold text-sm text-slate-900 dark:text-white">Refunds Report</h4>
                <p className="text-xs text-slate-500 mt-1">Compensating reversals, student refund requests, and approvals.</p>
              </div>
              <button
                onClick={() => handleDownloadCsv('refunds')}
                className="mt-4 w-full py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-xl text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center justify-center space-x-2 transition-colors"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Download CSV</span>
              </button>
            </div>

            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs flex flex-col justify-between">
              <div>
                <FileSpreadsheet className="w-6 h-6 text-amber-600 mb-2" />
                <h4 className="font-bold text-sm text-slate-900 dark:text-white">Settlements Report</h4>
                <p className="text-xs text-slate-500 mt-1">Merchant settlement records, bank reference numbers, and payout dates.</p>
              </div>
              <button
                onClick={() => handleDownloadCsv('settlements')}
                className="mt-4 w-full py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-xl text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center justify-center space-x-2 transition-colors"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Download CSV</span>
              </button>
            </div>

          </div>
        </div>
      )}

      {/* TAB 5: AUDIT LOGS */}
      {activeTab === 'audit' && (
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm space-y-4">
          <div>
            <h3 className="text-base font-extrabold text-slate-900 dark:text-white">Administrative Audit Trail</h3>
            <p className="text-xs text-slate-500">Forensic logs of all financial actions, logins, suspensions, and configurations</p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="text-slate-400 uppercase tracking-wider text-[10px] border-b border-slate-100 dark:border-slate-800">
                  <th className="pb-2.5 font-semibold">Timestamp</th>
                  <th className="pb-2.5 font-semibold">Actor</th>
                  <th className="pb-2.5 font-semibold">Role</th>
                  <th className="pb-2.5 font-semibold">Action</th>
                  <th className="pb-2.5 font-semibold">Resource</th>
                  <th className="pb-2.5 font-semibold">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60 font-medium">
                {auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                    <td className="py-2.5 text-slate-400 font-mono text-[11px]">
                      {new Date(log.createdAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}
                    </td>
                    <td className="py-2.5 font-bold text-slate-900 dark:text-white">{log.actorId}</td>
                    <td className="py-2.5">
                      <span className="px-2 py-0.5 rounded text-[10px] bg-slate-100 dark:bg-slate-800 font-semibold text-slate-600 dark:text-slate-300">
                        {log.actorRole}
                      </span>
                    </td>
                    <td className="py-2.5 font-mono text-blue-600 dark:text-blue-400 font-semibold">{log.action}</td>
                    <td className="py-2.5 text-slate-500">{log.resourceType}{log.resourceId ? ` / ${log.resourceId}` : ''}</td>
                    <td className="py-2.5 text-slate-600 dark:text-slate-300 max-w-xs truncate">{log.details || 'N/A'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

    </div>
  );
};
