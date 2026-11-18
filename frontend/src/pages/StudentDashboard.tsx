import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { Wallet, Transaction } from '../types';
import { RechargeModal } from '../components/RechargeModal';
import { ReceiptModal } from '../components/ReceiptModal';
import {
  Wallet as WalletIcon,
  PlusCircle,
  ArrowDownLeft,
  ArrowUpRight,
  RefreshCw,
  Search,
  Filter,
  CheckCircle2,
  Clock,
  AlertCircle,
  Receipt,
  RotateCcw,
  Store,
  Layers,
  Sparkles
} from 'lucide-react';

export const StudentDashboard: React.FC = () => {
  const { user } = useAuth();
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [filterType, setFilterType] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRechargeModalOpen, setIsRechargeModalOpen] = useState<boolean>(false);
  const [selectedReceiptTxn, setSelectedReceiptTxn] = useState<Transaction | null>(null);
  const [refundTxn, setRefundTxn] = useState<Transaction | null>(null);
  const [refundReason, setRefundReason] = useState<string>('');
  const [refundSubmitting, setRefundSubmitting] = useState<boolean>(false);
  const [refundMessage, setRefundMessage] = useState<string | null>(null);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [walletData, txnData] = await Promise.all([
        api.getWallet(),
        api.getWalletTransactions(0, 50)
      ]);
      setWallet(walletData);
      setTransactions(txnData.transactions || []);
    } catch (e) {
      console.error('Failed to load wallet data', e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  const filteredTransactions = transactions.filter(t => {
    if (filterType === 'ALL') return true;
    if (filterType === 'RECHARGE') return t.transactionType === 'RECHARGE';
    if (filterType === 'DEBIT') return t.transactionType === 'DEBIT';
    if (filterType === 'REFUND') return t.transactionType === 'REFUND';
    return true;
  });

  const totalRecharged = transactions
    .filter(t => t.transactionType === 'RECHARGE' && t.status === 'COMPLETED')
    .reduce((acc, t) => acc + t.amountMinorUnits, 0);

  const totalSpent = transactions
    .filter(t => t.transactionType === 'DEBIT' && t.status === 'COMPLETED')
    .reduce((acc, t) => acc + t.amountMinorUnits, 0);

  const handleRefundSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!refundTxn) return;
    setRefundSubmitting(true);
    setRefundMessage(null);
    try {
      await api.requestRefund(refundTxn.transactionId, refundTxn.amountMinorUnits, refundReason);
      setRefundMessage('Refund request submitted successfully for review.');
      setTimeout(() => {
        setRefundTxn(null);
        setRefundReason('');
        setRefundMessage(null);
      }, 2000);
    } catch (e: any) {
      setRefundMessage(`Error: ${e.message}`);
    } finally {
      setRefundSubmitting(false);
    }
  };

  return (
    <div className="space-y-4 sm:space-y-6 animate-in fade-in duration-150">
      
      {/* Top Section / Balance Display */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-3 sm:gap-5">
        
        {/* Minimal Clean Balance Card */}
        <div className="lg:col-span-2 rounded-2xl bg-white dark:bg-zinc-900 border border-stone-200 dark:border-zinc-800 p-5 sm:p-6 shadow-xs">
          <div className="flex flex-col justify-between h-full space-y-5">
            
            <div className="flex justify-between items-start gap-2">
              <div className="min-w-0">
                <span className="text-[11px] font-medium tracking-wide text-stone-400 dark:text-zinc-500 uppercase">Campus Wallet</span>
                <div className="flex flex-wrap items-center gap-2 mt-0.5">
                  <h1 className="text-lg sm:text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100 truncate">
                    {wallet?.student_name || user?.fullName}
                  </h1>
                  <span className="text-[10px] font-mono bg-stone-100 dark:bg-zinc-800 text-stone-600 dark:text-zinc-400 px-2 py-0.5 rounded border border-stone-200 dark:border-zinc-700">
                    {wallet?.student_id || user?.studentId}
                  </span>
                </div>
              </div>
              <span className="inline-flex items-center space-x-1.5 px-2 py-0.5 rounded text-[11px] font-medium bg-stone-100 dark:bg-zinc-800 text-stone-600 dark:text-zinc-300">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                <span className="capitalize">{wallet?.status || 'Active'}</span>
              </span>
            </div>

            <div className="py-1">
              <span className="text-xs text-stone-400 dark:text-zinc-500 font-medium">Available Balance</span>
              <div className="flex items-baseline flex-wrap gap-x-3 gap-y-1 mt-1">
                <div className="text-4xl sm:text-5xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
                  {wallet ? wallet.balance : 0}
                </div>
                <div className="text-base font-medium text-stone-500 dark:text-zinc-400">Tokens</div>
                <div className="text-xs text-stone-400 dark:text-zinc-500 font-normal pl-2 border-l border-stone-200 dark:border-zinc-800">
                  ≈ ₹{wallet ? wallet.balance : 0}.00 INR
                </div>
              </div>
            </div>

            <div className="pt-3 flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between border-t border-stone-100 dark:border-zinc-800/80">
              <span className="text-[10px] text-stone-400 dark:text-zinc-500 font-mono truncate">
                ID: {wallet?.wallet_id}
              </span>
              <button
                onClick={() => setIsRechargeModalOpen(true)}
                className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 font-semibold text-xs transition-all flex items-center justify-center space-x-2 touch-manipulation cursor-pointer shadow-xs active:scale-98"
              >
                <PlusCircle className="w-3.5 h-3.5" />
                <span>Recharge Balance</span>
              </button>
            </div>
          </div>
        </div>

        {/* Minimal Stats */}
        <div className="grid grid-cols-2 lg:grid-cols-1 gap-2.5 sm:gap-3">
          <div className="p-4 rounded-xl bg-white dark:bg-zinc-900 border border-stone-200 dark:border-zinc-800 shadow-xs">
            <div className="flex items-center justify-between text-[11px] text-stone-400 dark:text-zinc-500">
              <span>Total Recharged</span>
              <ArrowDownLeft className="w-3.5 h-3.5 text-stone-400 dark:text-zinc-500" />
            </div>
            <div className="text-lg sm:text-xl font-bold text-zinc-900 dark:text-zinc-100 mt-1">
              +{totalRecharged} <span className="text-xs font-normal text-stone-400">Tokens</span>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-white dark:bg-zinc-900 border border-stone-200 dark:border-zinc-800 shadow-xs">
            <div className="flex items-center justify-between text-[11px] text-stone-400 dark:text-zinc-500">
              <span>Total Spent</span>
              <ArrowUpRight className="w-3.5 h-3.5 text-stone-400 dark:text-zinc-500" />
            </div>
            <div className="text-lg sm:text-xl font-bold text-zinc-900 dark:text-zinc-100 mt-1">
              -{totalSpent} <span className="text-xs font-normal text-stone-400">Tokens</span>
            </div>
          </div>

          <div className="col-span-2 lg:col-span-1 p-3.5 rounded-xl bg-stone-50 dark:bg-zinc-900/60 border border-stone-200 dark:border-zinc-800 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Store className="w-3.5 h-3.5 text-stone-500 dark:text-zinc-400" />
              <div className="text-xs text-stone-600 dark:text-zinc-300">Canteen, Stationery, Transport</div>
            </div>
            <span className="text-[10px] font-medium text-emerald-600 dark:text-emerald-400">Online</span>
          </div>
        </div>

      </div>

      {/* Transaction History Section */}
      <div className="bg-white dark:bg-zinc-900 rounded-2xl border border-stone-200 dark:border-zinc-800 p-4 sm:p-5 shadow-xs">
        
        {/* Controls Bar */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 pb-3 border-b border-stone-100 dark:border-zinc-800">
          <div>
            <h2 className="text-sm sm:text-base font-bold text-zinc-900 dark:text-zinc-100">Transaction History</h2>
            <p className="text-[11px] text-stone-400 dark:text-zinc-500">Immutable ledger accounting records</p>
          </div>

          <div className="flex items-center justify-between w-full sm:w-auto gap-2">
            {/* Filter Pills */}
            <div className="flex overflow-x-auto no-scrollbar bg-stone-100 dark:bg-zinc-800/80 p-0.5 rounded-lg text-xs font-medium">
              {['ALL', 'RECHARGE', 'DEBIT', 'REFUND'].map((f) => (
                <button
                  key={f}
                  onClick={() => setFilterType(f)}
                  className={`px-2.5 py-1 rounded-md transition-all text-xs cursor-pointer ${filterType === f ? 'bg-white dark:bg-zinc-700 text-zinc-900 dark:text-zinc-100 shadow-xs' : 'text-stone-500 hover:text-stone-900 dark:text-zinc-400 dark:hover:text-zinc-200'}`}
                >
                  {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
                </button>
              ))}
            </div>

            <button
              onClick={loadData}
              className="p-1.5 rounded-lg text-stone-400 hover:text-stone-600 dark:hover:text-zinc-300 hover:bg-stone-100 dark:hover:bg-zinc-800 transition-colors shrink-0 cursor-pointer"
              title="Refresh"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Transactions Card List for Mobile & Table for Desktop */}
        <div className="mt-2">
          {isLoading ? (
            <div className="py-12 text-center text-xs text-stone-400">Loading ledger data...</div>
          ) : filteredTransactions.length === 0 ? (
            <div className="py-12 text-center">
              <Clock className="w-6 h-6 text-stone-300 dark:text-zinc-600 mx-auto mb-2" />
              <p className="text-xs font-medium text-stone-500">No transactions recorded</p>
            </div>
          ) : (
            <>
              {/* Mobile Card List */}
              <div className="block sm:hidden divide-y divide-stone-100 dark:divide-zinc-800/60">
                {filteredTransactions.map((txn) => {
                  const isCredit = txn.transactionType === 'RECHARGE' || txn.transactionType === 'REFUND' || txn.transactionType === 'CREDIT';
                  return (
                    <div key={txn.id} className="py-3 flex flex-col gap-1.5">
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex items-center space-x-2.5 min-w-0">
                          <div className="w-7 h-7 rounded-lg bg-stone-100 dark:bg-zinc-800 flex items-center justify-center shrink-0 text-stone-500 dark:text-zinc-400">
                            {isCredit ? <ArrowDownLeft className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" /> : <ArrowUpRight className="w-3.5 h-3.5 text-zinc-600 dark:text-zinc-300" />}
                          </div>
                          <div className="min-w-0">
                            <div className="font-semibold text-xs text-zinc-900 dark:text-zinc-100 truncate">
                              {txn.description || (isCredit ? 'Recharge' : 'Campus Purchase')}
                            </div>
                            <div className="text-[10px] text-stone-400">
                              {new Date(txn.createdAt).toLocaleDateString([], { month: 'short', day: 'numeric' })} • {new Date(txn.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </div>
                          </div>
                        </div>

                        <div className="text-right shrink-0">
                          <div className={`font-bold text-xs ${isCredit ? 'text-emerald-600 dark:text-emerald-400' : 'text-zinc-900 dark:text-zinc-100'}`}>
                            {isCredit ? '+' : '-'}{txn.amountMinorUnits}
                          </div>
                          <span className="text-[9px] text-stone-400 dark:text-zinc-500 uppercase tracking-wider">
                            {txn.status}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center justify-between text-[10px] text-stone-400 pt-1">
                        <span className="font-mono truncate max-w-[140px]">{txn.transactionId}</span>
                        <div className="flex space-x-1 shrink-0">
                          <button
                            onClick={() => setSelectedReceiptTxn(txn)}
                            className="px-2 py-0.5 text-[10px] font-medium text-stone-600 dark:text-zinc-300 hover:bg-stone-100 dark:hover:bg-zinc-800 rounded transition-colors"
                          >
                            Receipt
                          </button>
                          {txn.transactionType === 'DEBIT' && (
                            <button
                              onClick={() => setRefundTxn(txn)}
                              className="px-2 py-0.5 text-[10px] font-medium text-stone-500 hover:text-stone-800 rounded transition-colors"
                            >
                              Refund
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Desktop Minimal Table */}
              <div className="hidden sm:block overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="text-stone-400 dark:text-zinc-500 uppercase tracking-wider text-[10px] border-b border-stone-100 dark:border-zinc-800">
                      <th className="pb-2.5 font-medium">Type & Description</th>
                      <th className="pb-2.5 font-medium">Transaction ID</th>
                      <th className="pb-2.5 font-medium">Reference</th>
                      <th className="pb-2.5 font-medium">Amount</th>
                      <th className="pb-2.5 font-medium">Status</th>
                      <th className="pb-2.5 font-medium text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-stone-100 dark:divide-zinc-800/60 font-normal">
                    {filteredTransactions.map((txn) => {
                      const isCredit = txn.transactionType === 'RECHARGE' || txn.transactionType === 'REFUND' || txn.transactionType === 'CREDIT';
                      return (
                        <tr key={txn.id} className="hover:bg-stone-50/50 dark:hover:bg-zinc-800/30 transition-colors">
                          <td className="py-3 pr-3">
                            <div className="flex items-center space-x-2.5">
                              <div className="w-6 h-6 rounded bg-stone-100 dark:bg-zinc-800 flex items-center justify-center shrink-0">
                                {isCredit ? <ArrowDownLeft className="w-3 h-3 text-emerald-600 dark:text-emerald-400" /> : <ArrowUpRight className="w-3 h-3 text-zinc-600 dark:text-zinc-300" />}
                              </div>
                              <div>
                                <div className="font-semibold text-zinc-900 dark:text-zinc-100">
                                  {txn.description || (isCredit ? 'Recharge' : 'Campus Purchase')}
                                </div>
                                <div className="text-[10px] text-stone-400">
                                  {new Date(txn.createdAt).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })} at {new Date(txn.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </div>
                              </div>
                            </div>
                          </td>

                          <td className="py-3 font-mono text-[11px] text-stone-500 dark:text-zinc-400">
                            {txn.transactionId}
                          </td>

                          <td className="py-3 font-mono text-[11px] text-stone-400 dark:text-zinc-500">
                            {txn.referenceId || '—'}
                          </td>

                          <td className="py-3 font-semibold text-xs">
                            <span className={isCredit ? 'text-emerald-600 dark:text-emerald-400' : 'text-zinc-900 dark:text-zinc-100'}>
                              {isCredit ? '+' : '-'}{txn.amountMinorUnits} Tokens
                            </span>
                          </td>

                          <td className="py-3">
                            <span className="inline-flex items-center px-2 py-0.2 rounded text-[10px] font-medium bg-stone-100 dark:bg-zinc-800 text-stone-700 dark:text-zinc-300">
                              {txn.status}
                            </span>
                          </td>

                          <td className="py-3 text-right space-x-2">
                            <button
                              onClick={() => setSelectedReceiptTxn(txn)}
                              className="px-2 py-0.5 text-xs font-medium text-stone-600 hover:text-stone-900 dark:text-zinc-400 dark:hover:text-zinc-200 transition-colors"
                            >
                              Receipt
                            </button>
                            {txn.transactionType === 'DEBIT' && (
                              <button
                                onClick={() => setRefundTxn(txn)}
                                className="px-2 py-0.5 text-xs font-medium text-stone-400 hover:text-stone-600 dark:hover:text-zinc-300 transition-colors"
                              >
                                Refund
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>

      </div>

      {/* Modals */}
      <RechargeModal
        isOpen={isRechargeModalOpen}
        onClose={() => setIsRechargeModalOpen(false)}
        onSuccess={loadData}
      />

      <ReceiptModal
        transaction={selectedReceiptTxn}
        onClose={() => setSelectedReceiptTxn(null)}
      />

      {/* Refund Request Modal */}
      {refundTxn && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-slate-200 dark:border-slate-800">
            <h3 className="font-bold text-base text-slate-900 dark:text-white">Request Transaction Refund</h3>
            <p className="text-xs text-slate-500 mt-1">Transaction: <span className="font-mono">{refundTxn.transactionId}</span> ({refundTxn.amountMinorUnits} RITZ)</p>

            <form onSubmit={handleRefundSubmit} className="mt-4 space-y-3">
              <div>
                <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">Reason for refund</label>
                <textarea
                  required
                  rows={3}
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  placeholder="e.g. Canteen item out of stock / order cancelled"
                  className="w-full mt-1.5 p-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs focus:ring-2 focus:ring-blue-600 focus:outline-none"
                />
              </div>

              {refundMessage && (
                <div className={`p-2 rounded text-xs ${refundMessage.startsWith('Error') ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-600'}`}>
                  {refundMessage}
                </div>
              )}

              <div className="flex space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setRefundTxn(null)}
                  className="flex-1 py-2 rounded-xl text-xs font-semibold bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={refundSubmitting}
                  className="flex-1 py-2 rounded-xl text-xs font-bold bg-blue-600 hover:bg-blue-700 text-white disabled:opacity-50"
                >
                  {refundSubmitting ? 'Submitting...' : 'Submit Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};
