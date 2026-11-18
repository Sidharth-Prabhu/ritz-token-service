import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { Merchant, MerchantAccount, MerchantSettlement, Transaction } from '../types';
import {
  Store,
  ArrowUpRight,
  Clock,
  CheckCircle2,
  AlertCircle,
  FileText,
  DollarSign,
  Send,
  RefreshCw
} from 'lucide-react';

export const MerchantDashboard: React.FC = () => {
  const { user } = useAuth();
  const merchantId = user?.merchantId || 'TILLO';
  const [merchantData, setMerchantData] = useState<any>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [settlements, setSettlements] = useState<MerchantSettlement[]>([]);
  const [settleAmount, setSettleAmount] = useState<string>('');
  const [settleNotes, setSettleNotes] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const loadData = async () => {
    try {
      const [details, txns, settleList] = await Promise.all([
        api.getMerchantDetails(merchantId),
        api.getMerchantTransactions(merchantId, 0, 30),
        api.getMerchantSettlements(merchantId)
      ]);
      setMerchantData(details);
      setTransactions(txns.transactions || []);
      setSettlements(settleList || []);
    } catch (e) {
      console.error('Failed to load merchant data', e);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  const handleRequestSettlement = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!settleAmount || isNaN(Number(settleAmount))) return;
    setIsSubmitting(true);
    setStatusMessage(null);
    try {
      await api.requestSettlement(merchantId, Number(settleAmount), settleNotes);
      setStatusMessage('Settlement request submitted successfully to Finance Admin.');
      setSettleAmount('');
      setSettleNotes('');
      loadData();
    } catch (e: any) {
      setStatusMessage(`Error: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const account: MerchantAccount = merchantData?.account;

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      
      {/* Merchant Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center p-6 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm gap-4">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 rounded-2xl bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 flex items-center justify-center">
            <Store className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-xl font-extrabold text-slate-900 dark:text-white">
                {merchantData?.merchant?.name || 'Tillo Campus Canteen'}
              </h1>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 font-bold text-slate-600 dark:text-slate-300">
                {merchantId}
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              {merchantData?.merchant?.description || 'Campus Food & Cafeteria Settlement Terminal'}
            </p>
          </div>
        </div>

        <button
          onClick={loadData}
          className="p-2 rounded-xl text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          title="Refresh Data"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        
        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <div className="text-xs text-slate-500 font-semibold">Today's Sales</div>
          <div className="text-2xl font-black text-slate-900 dark:text-white mt-1.5">
            {merchantData?.today_sales || 0} <span className="text-xs font-semibold text-slate-400">RITZ</span>
          </div>
          <p className="text-[10px] text-emerald-600 mt-1 font-bold">Today's POS transactions</p>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <div className="text-xs text-slate-500 font-semibold">Total Cumulative Sales</div>
          <div className="text-2xl font-black text-slate-900 dark:text-white mt-1.5">
            {account ? account.totalEarnedMinorUnits : 0} <span className="text-xs font-semibold text-slate-400">RITZ</span>
          </div>
          <p className="text-[10px] text-slate-400 mt-1">Lifetime tokens processed</p>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <div className="text-xs text-slate-500 font-semibold">Pending Settlement</div>
          <div className="text-2xl font-black text-amber-600 mt-1.5">
            {account ? account.pendingSettlementMinorUnits : 0} <span className="text-xs font-semibold text-slate-400">RITZ</span>
          </div>
          <p className="text-[10px] text-amber-600 mt-1 font-bold">Ready for bank transfer</p>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <div className="text-xs text-slate-500 font-semibold">Settled to Bank</div>
          <div className="text-2xl font-black text-emerald-600 mt-1.5">
            ₹{account ? account.settledMinorUnits : 0}
          </div>
          <p className="text-[10px] text-slate-400 mt-1">Confirmed bank payouts</p>
        </div>

      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Merchant Recent Transactions */}
        <div className="lg:col-span-2 bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
          <div className="flex justify-between items-center mb-4">
            <div>
              <h3 className="font-extrabold text-slate-900 dark:text-white text-base">Recent Sales Transactions</h3>
              <p className="text-xs text-slate-500">Real-time debits processed at this merchant terminal</p>
            </div>
            <span className="text-xs font-bold text-slate-500">{transactions.length} Records</span>
          </div>

          <div className="overflow-x-auto">
            {transactions.length === 0 ? (
              <div className="py-12 text-center text-xs text-slate-400">No transactions yet</div>
            ) : (
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="text-slate-400 uppercase tracking-wider text-[10px] border-b border-slate-100 dark:border-slate-800">
                    <th className="pb-2.5 font-semibold">Time</th>
                    <th className="pb-2.5 font-semibold">Student ID</th>
                    <th className="pb-2.5 font-semibold">Reference</th>
                    <th className="pb-2.5 font-semibold">Amount</th>
                    <th className="pb-2.5 font-semibold">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60 font-medium">
                  {transactions.map((t) => (
                    <tr key={t.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                      <td className="py-2.5 text-slate-400">
                        {new Date(t.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </td>
                      <td className="py-2.5 font-mono font-bold text-slate-900 dark:text-white">{t.studentId}</td>
                      <td className="py-2.5 font-mono text-slate-500">{t.referenceId || 'N/A'}</td>
                      <td className="py-2.5 font-black text-slate-900 dark:text-white">+{t.amountMinorUnits} RITZ</td>
                      <td className="py-2.5">
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300">
                          {t.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Settlement Request Column */}
        <div className="space-y-6">
          
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
            <h3 className="font-extrabold text-slate-900 dark:text-white text-base">Request Settlement</h3>
            <p className="text-xs text-slate-500 mt-1">Initiate bank payout for accumulated wallet revenue</p>

            <form onSubmit={handleRequestSettlement} className="mt-4 space-y-3">
              <div>
                <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">Settlement Amount (RITZ)</label>
                <div className="relative mt-1">
                  <input
                    type="number"
                    max={account?.pendingSettlementMinorUnits || 0}
                    min="10"
                    placeholder={`Max: ${account?.pendingSettlementMinorUnits || 0}`}
                    value={settleAmount}
                    onChange={(e) => setSettleAmount(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs font-bold focus:ring-2 focus:ring-blue-600 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">Notes / Remarks</label>
                <input
                  type="text"
                  placeholder="e.g. Weekly canteen payout"
                  value={settleNotes}
                  onChange={(e) => setSettleNotes(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs focus:ring-2 focus:ring-blue-600 focus:outline-none"
                />
              </div>

              {statusMessage && (
                <div className={`p-2 rounded-lg text-xs ${statusMessage.startsWith('Error') ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-600'}`}>
                  {statusMessage}
                </div>
              )}

              <button
                type="submit"
                disabled={isSubmitting || !account || account.pendingSettlementMinorUnits <= 0}
                className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold flex items-center justify-center space-x-2 shadow-md disabled:opacity-50 transition-all"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{isSubmitting ? 'Submitting...' : 'Request Payout'}</span>
              </button>
            </form>
          </div>

          {/* Past Settlements List */}
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
            <h4 className="font-extrabold text-sm text-slate-900 dark:text-white mb-3">Settlement History</h4>
            {settlements.length === 0 ? (
              <div className="text-center py-6 text-xs text-slate-400">No settlement requests yet</div>
            ) : (
              <div className="space-y-2.5 max-h-60 overflow-y-auto">
                {settlements.map((s) => (
                  <div key={s.id} className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-100 dark:border-slate-800 text-xs">
                    <div className="flex justify-between font-bold">
                      <span className="font-mono text-slate-900 dark:text-white">{s.settlementId}</span>
                      <span className="text-blue-600">₹{s.amountMinorUnits}</span>
                    </div>
                    <div className="flex justify-between items-center mt-1 text-[11px] text-slate-500">
                      <span>Status: <span className="font-bold text-slate-700 dark:text-slate-300">{s.status}</span></span>
                      {s.bankReference && <span className="font-mono text-emerald-600">Ref: {s.bankReference}</span>}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

        </div>

      </div>

    </div>
  );
};
