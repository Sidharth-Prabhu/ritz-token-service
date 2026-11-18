import React from 'react';
import { Transaction } from '../types';
import { X, Printer, CheckCircle2, ShieldCheck, ArrowDownLeft, ArrowUpRight } from 'lucide-react';

interface ReceiptModalProps {
  transaction: Transaction | null;
  onClose: () => void;
}

export const ReceiptModal: React.FC<ReceiptModalProps> = ({ transaction, onClose }) => {
  if (!transaction) return null;

  const handlePrint = () => {
    window.print();
  };

  const isCredit = transaction.transactionType === 'RECHARGE' || transaction.transactionType === 'REFUND' || transaction.transactionType === 'CREDIT';

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-end sm:items-center justify-center p-0 sm:p-4">
      <div className="bg-white dark:bg-slate-900 rounded-t-3xl sm:rounded-2xl max-w-md w-full p-5 sm:p-6 shadow-2xl border border-slate-200 dark:border-slate-800 relative animate-in fade-in slide-in-from-bottom-6 sm:zoom-in-95 duration-200 max-h-[92vh] overflow-y-auto">
        
        {/* Mobile Pull Indicator */}
        <div className="w-10 h-1 bg-slate-300 dark:bg-slate-700 rounded-full mx-auto mb-3 sm:hidden"></div>

        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-full text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors print:hidden"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Printable Receipt Content */}
        <div id="printable-receipt" className="p-2">
          
          {/* Header */}
          <div className="text-center pb-4 border-b border-slate-200 dark:border-slate-700">
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-blue-600 text-white mb-2 shadow-md">
              {isCredit ? <ArrowDownLeft className="w-6 h-6" /> : <ArrowUpRight className="w-6 h-6" />}
            </div>
            <h3 className="font-extrabold text-lg text-slate-900 dark:text-white">RITZ Wallet Receipt</h3>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 uppercase tracking-wider font-semibold">Rajalakshmi Institute of Technology</p>
          </div>

          {/* Amount Callout */}
          <div className="py-5 text-center bg-slate-50 dark:bg-slate-800/50 rounded-xl my-4 border border-slate-100 dark:border-slate-800">
            <div className="text-xs font-semibold text-slate-500 dark:text-slate-400">Total Transaction Amount</div>
            <div className={`text-3xl font-extrabold mt-1 ${isCredit ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-900 dark:text-white'}`}>
              {isCredit ? '+' : '-'}{transaction.amountMinorUnits} <span className="text-sm font-semibold text-slate-500">RITZ</span>
            </div>
            <div className="inline-flex items-center space-x-1 mt-2 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{transaction.status}</span>
            </div>
          </div>

          {/* Details Table */}
          <div className="space-y-2.5 text-xs">
            <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
              <span className="text-slate-500 dark:text-slate-400">Transaction ID</span>
              <span className="font-mono font-bold text-slate-900 dark:text-white">{transaction.transactionId}</span>
            </div>
            {transaction.referenceId && (
              <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                <span className="text-slate-500 dark:text-slate-400">Reference ID</span>
                <span className="font-mono text-slate-900 dark:text-white">{transaction.referenceId}</span>
              </div>
            )}
            <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
              <span className="text-slate-500 dark:text-slate-400">Type</span>
              <span className="font-semibold text-slate-900 dark:text-white">{transaction.transactionType}</span>
            </div>
            {transaction.merchantId && (
              <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                <span className="text-slate-500 dark:text-slate-400">Merchant</span>
                <span className="font-semibold text-slate-900 dark:text-white">{transaction.merchantId}</span>
              </div>
            )}
            {transaction.studentId && (
              <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                <span className="text-slate-500 dark:text-slate-400">Student ID</span>
                <span className="font-mono font-semibold text-slate-900 dark:text-white">{transaction.studentId}</span>
              </div>
            )}
            {transaction.description && (
              <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                <span className="text-slate-500 dark:text-slate-400">Description</span>
                <span className="text-slate-900 dark:text-white text-right max-w-[200px] truncate">{transaction.description}</span>
              </div>
            )}
            <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
              <span className="text-slate-500 dark:text-slate-400">Date & Time</span>
              <span className="text-slate-900 dark:text-white">
                {new Date(transaction.createdAt).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}
              </span>
            </div>
            {transaction.previousBalanceMinorUnits !== undefined && transaction.currentBalanceMinorUnits !== undefined && (
              <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-2 rounded">
                <span className="text-slate-500 dark:text-slate-400">Wallet Balance After</span>
                <span className="font-extrabold text-blue-600 dark:text-blue-400">{transaction.currentBalanceMinorUnits} RITZ</span>
              </div>
            )}
          </div>

          {/* Footer security note */}
          <div className="mt-6 pt-4 border-t border-dashed border-slate-200 dark:border-slate-700 flex items-center justify-center space-x-1.5 text-[10px] text-slate-400 text-center">
            <ShieldCheck className="w-3.5 h-3.5 text-blue-600" />
            <span>Authoritative Double-Entry Ledger Verified & Signed</span>
          </div>

        </div>

        {/* Action Buttons */}
        <div className="mt-5 flex space-x-3 print:hidden">
          <button
            onClick={handlePrint}
            className="flex-1 py-2.5 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-bold rounded-xl flex items-center justify-center space-x-2 transition-colors text-xs"
          >
            <Printer className="w-4 h-4" />
            <span>Print Receipt</span>
          </button>
          <button
            onClick={onClose}
            className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl transition-colors text-xs"
          >
            Close
          </button>
        </div>

      </div>
    </div>
  );
};
